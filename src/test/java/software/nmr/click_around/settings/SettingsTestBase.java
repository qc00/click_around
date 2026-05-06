package software.nmr.click_around.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

abstract class SettingsTestBase {
    AppSettings app;
    ProjectSettings project;

    @BeforeEach
    void setUp() {
        app = AppSettings.testOverride = new AppSettings();
        project = new ProjectSettings();
    }

    @AfterEach
    void cleanup() {
        AppSettings.testOverride = null;
    }

    public static NavigationRule exampleRule() {
        return new NavigationRule(new Xml("ns", "tag", ""), new JavaAnnotation("com.x.Y", "value"));
    }

    public static NavigationRule wildcardTag(String attr) {
        return new NavigationRule(new Xml("", "*", attr), new JavaAnnotation("com.x.Y", "value"));
    }

    public static @NotNull AppSettings roundTrip(AppSettings s) {
        // Tests both IntelliJ & our own serialization
        var serialized = XmlSerializer.serialize(s.getState());
        var out = new AppSettings();
        out.loadState(XmlSerializer.deserialize(serialized, AbsSettings.State.class));
        return out;
    }
}
