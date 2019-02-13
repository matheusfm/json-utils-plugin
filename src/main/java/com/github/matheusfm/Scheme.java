package com.github.matheusfm;

import com.intellij.openapi.diagnostic.Logger;
import org.fife.ui.rsyntaxtextarea.Theme;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

/**
 * Represents the default schemes defined in lib into /org/fife/ui/rsyntaxtextarea/themes/
 */
public enum Scheme {

    /**
     * default schemes
     */
    DARK("dark.xml"),
    DEFAULT("default.xml"),
    DEFAULT_ALT("default-alt.xml"),
    ECLIPSE("eclipse.xml"),
    IDEA("idea.xml"),
    MONOKAI("monokai.xml"),
    VS("vs.xml");

    private static final Logger log = Logger.getInstance(Scheme.class);

    private static final Set<String> NAMES = Arrays.stream(values()).map(Enum::name).collect(toSet());

    protected static final Scheme[] SORTED_VALUES = Arrays.stream(Scheme.values())
            .sorted(comparing(Enum::name))
            .toArray(Scheme[]::new);

    private static final String BASE_PACKAGE = "/org/fife/ui/rsyntaxtextarea/themes/";


    public static final Scheme DEFAULT_SCHEME = UIManager.getLookAndFeel().getDescription().contains("Dark") ? DARK : IDEA;

    private final String fileName;

    Scheme(String fileName) {
        this.fileName = fileName;
    }

    private String getClassPathFileName() {
        return BASE_PACKAGE + fileName;
    }

    public String getLabel() {
        return name().toLowerCase();
    }

    public static Scheme getScheme(Object schemeStr) {
        String schemeName;

        if (schemeStr == null || !String.class.isAssignableFrom(schemeStr.getClass())) {
            return DEFAULT_SCHEME;
        }

        schemeName = schemeStr.toString().toUpperCase();
        if (!NAMES.contains(schemeName)) {
            return MONOKAI;
        }

        return Scheme.valueOf(schemeName);
    }

    public Theme loadTheme() {
        try {
            return Theme.load(getClass().getResourceAsStream(getClassPathFileName()));
        } catch (IOException ioe) {
            log.error(ioe);
            throw new IllegalStateException(ioe);
        }
    }
}
