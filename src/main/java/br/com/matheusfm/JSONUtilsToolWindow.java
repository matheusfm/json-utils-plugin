package br.com.matheusfm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.jayway.jsonpath.JsonPath;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author Matheus Moraes
 */
public class JSONUtilsToolWindow implements ToolWindowFactory {
    private JTextArea txtJson;
    private JButton btFormat;
    private JTextField txtPath;
    private JLabel label;
    private JPanel toolWindowContent;
    private JTextArea txtResult;
    private JLabel lbResult;
    private JButton btCopy;
    private JButton btPaste;
    private Gson gson;

    private String mJson;
    private String mJsonPath;

    public JSONUtilsToolWindow() {
        gson = new GsonBuilder().setPrettyPrinting().create();

        updateJsonTexts();

        btFormat.addActionListener(event -> {
            updateJsonTexts();
            label.setText("");
            try {
                txtJson.setText(getPrettyJson(mJson));
            } catch (Exception e) {
                label.setText(" Invalid JSON");
            }
        });

        txtPath.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                evaluate();
            }
        });

        btCopy.addActionListener(event -> {
            updateJsonTexts();
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(mJsonPath), null);
            } catch (Exception e) {
                label.setText(" Error copying");
            }
        });

        btPaste.addActionListener(e -> {
            try {
                txtJson.setText(getPrettyJson(Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString()));
            } catch (Exception e1) {
                txtJson.setText("");
            }
            evaluate();
        });

        txtJson.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                evaluate();
            }
        });
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
    private String getPrettyJson(Object json) {
        return json != null ?
                json instanceof String ?
                        gson.toJson(JsonParser.parseString(json.toString())) :
                        gson.toJson(json, json.getClass())
                : "";
    }

    /**
     * @param json     JSON text
     * @param jsonPath JSON Path expression
     * @return pretty JSON
     */
    private String getPrettyJson(String json, String jsonPath) {
        try {
            return getPrettyJson(JsonPath.read(json, jsonPath));
        } catch (Exception e) {
            return "No match";
        }
    }

    /**
     * evaluate JSONPath expression and update jTextArea with result
     */
    private void evaluate() {
        updateJsonTexts();
        txtResult.setText(getPrettyJson(mJson, mJsonPath));
    }

    /**
     * update JSONPath and JSON Strings with related jTextFields
     */
    private void updateJsonTexts() {
        mJson = txtJson.getText();
        mJsonPath = txtPath.getText();
    }
}
