package com.github.matheusfm;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import lombok.experimental.UtilityClass;
import org.jdom.Element;

import java.util.List;

import static java.util.Arrays.asList;

@UtilityClass
class Constants {

    static final String DEMO_JSON = applicationInfo("{\n" +
            "  \"product\": {\n" +
            "    \"name\": \"{productName}\",\n" +
            "    \"description\": \"{description}\",\n" +
            "    \"version\": \"{version}\",\n" +
            "    \"releaseDate\": \"20XX-XX-XXT00:00:00.000Z\",\n" +
            "    \"dateOfBirth\": \"2000-XX-XXT00:00:00.000Z\",\n" +
            "    \"website\": \"https://github.com/matheusfm/json-utils-plugin\",\n" +
            "    \"phone\": \"555-1212\",\n" +
            "    \"authenticity\": \"genuine\"\n" +
            "  },\n" +
            "  \"author\": {\n" +
            "    \"name\": \"{authorName}\",\n" +
            "    \"email\": \"{authorEmail}\"\n" +
            "  },\n" +
            "  \"Usage\": {\n" +
            "    \"Formatting\": {\n" +
            "      \"first\": \"Paste the code into the Formatter field\",\n" +
            "      \"then\": \"Use Paste Button on the bottom-right corner of the Tool Window.\"\n" +
            "    }\n" +
            "  }\n" +
            "}");

    static final String DEFAULT_PATH_EXPRESSION = "$";

    static final String NOTIFICATION_GROUP_DISPLAY_ID = "JSON-Utils Plugin";

    public static final String PLUGIN_DESCRIPTOR = "json-utils-plugin";
    private static final String PROPERTY_SCHEME_NAME = PLUGIN_DESCRIPTOR + ".scheme";

    private static String applicationInfo(String template) {

        String resource = "/META-INF/plugin.xml";

        StringBuilder buffer = new StringBuilder(template);
        try {
            Element rootElement = JDOMUtil.load(JSONUtilsToolWindow.class, resource);

            String productName = rootElement.getChild("name").getValue();
            String version = rootElement.getChild("version").getValue();

            Element vendor = rootElement.getChild("vendor");
            String authorName = vendor.getValue();
            String authorEmail = vendor.getAttributeValue("email");

            String description = rootElement.getChild("description").getValue();

            // create attribute list to replace
            List<Pair<String, String>> attributes = asList(
                    new Pair<>("{productName}", productName),
                    new Pair<>("{version}", version),
                    new Pair<>("{authorName}", authorName),
                    new Pair<>("{authorEmail}", authorEmail),
                    new Pair<>("{description}", description.trim()));

            // replace list in template
            for (Pair<String, String> pair : attributes) {
                int start = buffer.indexOf(pair.first);
                buffer.replace(
                        start,
                        Math.max(0, start) + pair.first.length(),
                        pair.second
                );
            }
        } catch (Exception var4) {
            throw new IllegalStateException("Cannot load resource: " + resource, var4);
        }

        return buffer.toString();
    }

    static Scheme getPreferenceScheme() {
        return Scheme.getScheme(PropertiesComponent.getInstance().getValue(PROPERTY_SCHEME_NAME, Scheme.DEFAULT_SCHEME.getLabel()));
    }

    static void setPreferenceScheme(Scheme scheme) {
        PropertiesComponent.getInstance().setValue(PROPERTY_SCHEME_NAME, scheme.getLabel());
    }
}
