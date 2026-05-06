package software.nmr.click_around.settings;

import org.junit.jupiter.api.Test;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static software.nmr.click_around.settings.SettingsTestBase.exampleRule;

class NavigationRuleTest {

    @Test
    void equalsAndHashCodeAreContentBased() {
        NavigationRule a = new NavigationRule(new Xml("1", "2", "3"), new JavaAnnotation("4", "5"));
        NavigationRule b = new NavigationRule(new Xml("1", "2", "3"), new JavaAnnotation("4", "5"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(new Object(), a);
        assertNotEquals(null, a);

        assertNotEquals(a, new NavigationRule(a.usage, Set.of(new JavaAnnotation("4", "7"))));
        assertNotEquals(a, new NavigationRule(Set.of(new Xml("1", "2", "2")), b.definition));
    }

    @Test
    void equalsAndHashCodeIncludeAllCandidates() {
        var a = new NavigationRule(
                Set.of(new Xml("u1", "u2", "u3"), new JavaAnnotation("u4", "u5")),
                Set.of(new Xml("d1", "d2", "d3"), new JavaAnnotation("d4", "d5")));
        var b = new NavigationRule(
                Set.of(new Xml("u1", "u2", "u3"), new JavaAnnotation("u4", "u5")),
                Set.of(new Xml("d1", "d2", "d3"), new JavaAnnotation("d4", "d5")));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new NavigationRule(Set.of(a.usage.iterator().next()), a.definition));
    }

    @Test
    void navigationRuleSerializes() {
        NavigationRule rule = exampleRule();
        var settings = new AppSettings();
        settings.rules.add(rule);

        var restored = SettingsTestBase.roundTrip(settings);

        assertEquals(settings.rules, restored.rules);
    }
}
