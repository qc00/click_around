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

            target.loadState(source);

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
}
