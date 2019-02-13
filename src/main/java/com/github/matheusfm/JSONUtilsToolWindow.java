package com.github.matheusfm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.jayway.jsonpath.JsonPath;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Arrays.asList;

/**
 * @author Matheus Moraes
 */
public class JSONUtilsToolWindow implements ToolWindowFactory {

    public static final Logger log = Logger.getInstance(JSONUtilsToolWindow.class);

    private Gson gson;
    private JButton btClear;
    private JButton btCopy;
    private JButton btFormat;
    private JButton btNextScheme;
    private JButton btPaste;
    private JButton btPreviousScheme;

    private JComboBox<String> cbSchemes;

    private JLabel lbResult;
    private JPanel toolWindowContent;

    private JsonParser jsonParser;
    private JTextField txtPath;

    private RSyntaxTextArea textAreaDest;
    private RSyntaxTextArea textAreaSource;
    private RTextScrollPane scrollPaneDest;
    private RTextScrollPane scrollPaneSource;

    private String mJsonPath;
    private String mJsonSource;

    private Set<RSyntaxTextArea> textAreas;

    public JSONUtilsToolWindow() {
        buildUI();
    }

    private void buildUI() {
        initFields();

        updateJsonTexts();

        initListeners();

        loadSchemesComboBox();

        // TODO: lookup for current Scheme Loaded eg. Dark o Intellij?


        Scheme preferenceScheme = Constants.getPreferenceScheme();
        cbSchemes.setSelectedItem(preferenceScheme.getLabel());

        configTextArea(preferenceScheme);
    }

    private void initFields() {
        textAreas = new HashSet<>(asList(textAreaSource, textAreaDest));
        gson = new GsonBuilder().setPrettyPrinting().create();
        jsonParser = new JsonParser();


        txtPath.setText(Constants.DEFAULT_PATH_EXPRESSION);
        textAreaSource.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textAreaSource.setText(Constants.DEMO_JSON);
        textAreaSource.setCaretPosition(0);

        for (RTextScrollPane scrollPane : asList(scrollPaneSource, scrollPaneDest)) {
            scrollPane.setLineNumbersEnabled(true);
            scrollPane.setFoldIndicatorEnabled(true);
        }
    }

    private void initListeners() {
        btFormat.addActionListener(event -> {
            updateTextAreaSource(mJsonSource);
            lbResult.setText("");
        });

        txtPath.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (VK_ENTER == event.getKeyCode()) {
                    evaluate();
                }
            }
        });

        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        btCopy.addActionListener(event -> {
            updateJsonTexts();
            try {
                systemClipboard.setContents(new StringSelection(mJsonPath), null);
            } catch (Exception e) {
                Notifications.Bus.notify(
                        new Notification(
                                Constants.NOTIFICATION_GROUP_DISPLAY_ID,
                                Constants.NOTIFICATION_GROUP_DISPLAY_ID + ": Error copying",
                                e.getMessage(),
                                NotificationType.ERROR
                        )
                );
            }
        });

        btPaste.addActionListener(event -> {
            try {
                if (systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    String clipboardContent = systemClipboard.getData(DataFlavor.stringFlavor).toString();
                    updateTextAreaSource(clipboardContent);
                }
            } catch (UnsupportedFlavorException | IOException e) {
                Notifications.Bus.notify(
                        new Notification(
                                Constants.NOTIFICATION_GROUP_DISPLAY_ID,
                                Constants.NOTIFICATION_GROUP_DISPLAY_ID + ": Error pasting",
                                e.getMessage(),
                                NotificationType.ERROR
                        )
                );
            }
        });

        btClear.addActionListener(e -> updateTextAreaSource(""));

        btPreviousScheme.addActionListener(e -> cbSchemes.setSelectedIndex(getInfiniteStep(-1)));
        btNextScheme.addActionListener(e -> cbSchemes.setSelectedIndex(getInfiniteStep(+1)));

        textAreaSource.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent documentEvent) {
                evaluate();
            }
        });
    }

    private void updateTextAreaSource(String jsonSource) {
        try {
            String prettyJson = formatToPrettyJson(jsonSource);
            textAreaSource.setText(prettyJson);
            textAreaSource.setCaretPosition(0);

            evaluate();

        } catch (Exception e) {
            // last chance a least to show the json source text
            textAreaSource.setText(jsonSource);
            textAreaSource.setCaretPosition(0);
            Notifications.Bus.notify(
                    new Notification(
                            Constants.NOTIFICATION_GROUP_DISPLAY_ID,
                            Constants.NOTIFICATION_GROUP_DISPLAY_ID + ": Error updateTextAreaSource",
                            e.getMessage(),
                            NotificationType.ERROR
                    )
            );
        }
    }

    private int getInfiniteStep(int step) {
        return getInfiniteStep(step, 0, cbSchemes.getItemCount(), cbSchemes.getSelectedIndex());
    }

    /**
     * Default Schemes Elements
     * _________________________
     * +--> 0 1 2 3 4 5 6 7 >--+
     * +-<--<---<---<----<--<--+
     */
    private static int getInfiniteStep(int step, int min, int max, int index) {

        int currentIndex = index + step;

        if (currentIndex >= max) {
            return min;
        }

        if (currentIndex < min) {
            return max - 1;
        }

        return currentIndex;
    }

    private void loadSchemesComboBox() {

        for (Scheme scheme : Scheme.SORTED_VALUES) {
            cbSchemes.addItem(scheme.getLabel());
        }

        cbSchemes.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Scheme scheme = Scheme.getScheme(e.getItem());
                // Save preferences
                Constants.setPreferenceScheme(scheme);
                configTextArea(scheme);
            }
        });
    }


    private void configTextArea(Scheme scheme) {
        Theme theme = scheme.loadTheme();

        for (RSyntaxTextArea textArea : textAreas) {
            theme.apply(textArea);
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            textArea.setCodeFoldingEnabled(true);
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * @param json JSON text
     * @return pretty JSON
     */
    private String formatToPrettyJson(Object json) {
        if (json == null || "null".equals(json) || "".equals(json)) {
            return "";
        }

        if (json instanceof String) {
            return gson.toJson(jsonParser.parse(json.toString()));
        }

        String ret = gson.toJson(json, json.getClass());

        return !ret.isEmpty() ? ret : "bad content";
    }

    /**
     * evaluate JSONPath expression and update jTextArea with result
     */
    private void evaluate() {
        updateJsonTexts();

        try {
            String prettyJson = formatToPrettyJson(JsonPath.read(mJsonSource, mJsonPath));
            if (prettyJson != null && !prettyJson.isEmpty()) {
                textAreaDest.setText(prettyJson);
            }
        } catch (Exception e) {
            if (mJsonSource != null && !mJsonSource.isEmpty()) {
                log.error(String.format("Error trying to set destination text with: [%s]", mJsonSource));
            }
        }
    }

    /**
     * update JSONPath and JSON Strings with related jTextFields
     */
    private void updateJsonTexts() {
        mJsonSource = textAreaSource.getText();
        mJsonPath = txtPath.getText();
    }
}
