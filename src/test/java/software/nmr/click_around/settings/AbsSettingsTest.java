package software.nmr.click_around.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static software.nmr.click_around.settings.SettingsTestBase.*;

/**
 * Tests for {@link AbsSettings} contract using {@link AppSettings} as concrete impl.
 */
class AbsSettingsTest {

    @Test
    void loadStateReplacesRulesAndIncrementsVersion() {
        var target = new AppSettings();
        for (int i = 1; i >= 0; i--) {
            int vBefore = target.ruleVersion.get();

            var source = new AppSettings();
            source.rules.add(exampleRule());
            if (i == 0) {
                source.rules.add(wildcardTag("a"));
            }

            target.loadState(source.getState());

            assertEquals(target.rules, source.rules);
            assertTrue(target.ruleVersion.get() > vBefore);
            assertNotEquals(target.ruleVersion.get(), source.ruleVersion.get());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void populatedSettingsRoundtrips(int num) {
        var s = new AppSettings();
        switch (num) {
            case 3: // IntelliJ has special rule for two items....
                s.rules.add(wildcardTag("a"));
                s.rules.add(wildcardTag("b"));
            case 1:
                s.rules.add(exampleRule());
            case 0:
        }

        var restored = roundTrip(s);
        assertEquals(s.rules, restored.rules);
    }

    @Test
    void schemaValidationRejectsMissingRequiredRuleContent() {
        var invalid = """
                <rules>
                    <rule type="TO_DEFINITION"/>
                </rules>""";

        assertThrows(Exception.class, () -> SettingsSchema.validate(invalid));
    }

    @Test
    void schemaValidationAcceptsGeneratedSettingsXml() {
        var settings = new AppSettings();
        settings.rules.add(exampleRule());

        assertDoesNotThrow(() -> SettingsSchema.validate(settings.getState().xml));
    }

    @Test
    void loadStateWithInvalidXmlSetsErrorComment() {
        var target = new AppSettings();
        var state = new AbsSettings.State();
        state.xml = "<not-valid-root/>";
        target.loadState(state);

        assertTrue(target.rules.isEmpty());
        assertTrue(target.getState().xml.contains("Config unmarshalling failed"));
        assertTrue(target.getState().xml.contains("<not-valid-root/>"));
    }

    @Test
    void loadStateIgnoresNullOrEmptyXml() {
        var target = new AppSettings();
        target.rules.add(exampleRule());
        int vBefore = target.ruleVersion.get();

        var emptyState = new AbsSettings.State();
        emptyState.xml = null;
        target.loadState(emptyState);
        assertEquals(1, target.rules.size(), "null xml should not change rules");

        emptyState.xml = "";
        target.loadState(emptyState);
        assertEquals(1, target.rules.size(), "empty xml should not change rules");
    }

    @Test
    void setRulesWithEmptyStringClearsRules() {
        var target = new AppSettings();
        target.rules.add(exampleRule());
        target.notifyRules();

        var result = target.setRules("   ");
        assertNull(result);
        assertTrue(target.rules.isEmpty());
    }

    @Test
    void setRulesReturnsExceptionOnInvalidXml() {
        var target = new AppSettings();
        var result = target.setRules("<invalid/>");
        assertNotNull(result);
    }

    @Test
    void getStateGeneratesXmlOnFirstAccess() {
        var s = new AppSettings();
        s.rules.add(exampleRule());
        var state = s.getState();
        assertNotNull(state.xml);
        assertTrue(state.xml.contains("rules"));
    }

    @Test
    void notifyRulesIncrementsVersion() {
        var s = new AppSettings();
        int v1 = s.ruleVersion.get();
        s.notifyRules();
        assertTrue(s.ruleVersion.get() > v1);
    }
}
