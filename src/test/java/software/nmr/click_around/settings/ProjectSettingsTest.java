package software.nmr.click_around.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSettingsTest extends SettingsTestBase {

    private void setupAppAndProjectRules() {
        app.rules.add(new NavigationRule(new Xml("ns", "appTag", ""), new JavaAnnotation("com.App", "value")));
        app.notifyRules();
        project.rules.add(new NavigationRule(new Xml("ns", "projTag", ""), new JavaAnnotation("com.Proj", "value")));
        project.notifyRules();
    }

    @Test
    void indexContainsBothAppAndProjectRules() {
        setupAppAndProjectRules();

        var index = project.getIndexByXml();
        assertNotNull(index.get("ns").get("appTag"), "app rule should be in index");
        assertNotNull(index.get("ns").get("projTag"), "project rule should be in index");
    }

    @ParameterizedTest
    @ValueSource(strings = {"appTag", "projTag"})
    void appNotifyInvalidatesCache(String remove) {
        setupAppAndProjectRules();
        var index1 = project.getIndexByXml();

        var removeFrom = remove.equals("appTag") ? app : project;
        removeFrom.rules.clear();
        removeFrom.notifyRules();

        var index2 = project.getIndexByXml();
        assertNotSame(index1, index2, "bumping version must cause recompute");
        assertNull(index2.get("ns").get(remove), "cleared rules should disappear from new index");
    }

    @Test
    void indexIsCachedForSameVersion() {
        app.rules.add(exampleRule());
        app.notifyRules();

        var index1 = project.getIndexByXml();
        var index2 = project.getIndexByXml();
        assertSame(index1, index2, "index must be cached when version has not changed");
    }

    @Test
    void loadStateOnProjectBumpsVersionAndInvalidatesCache() {
        project.rules.add(exampleRule());
        project.notifyRules();
        var index1 = project.getIndexByXml();

        var incoming = new ProjectSettings();
        incoming.rules.add(new NavigationRule(new Xml("ns", "new", ""), new JavaAnnotation("com.New", "val")));
        project.loadState(incoming);

        var index2 = project.getIndexByXml();
        assertNotSame(index1, index2);
        assertNotNull(index2.get("ns").get("new"));
    }

    @Test
    void emptyRulesProduceEmptyIndex() {
        var index = project.getIndexByXml();
        assertTrue(index.isEmpty());
    }
}
