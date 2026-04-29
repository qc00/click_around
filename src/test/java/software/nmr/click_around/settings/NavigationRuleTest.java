package software.nmr.click_around.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import static org.junit.jupiter.api.Assertions.*;
import static software.nmr.click_around.settings.SettingsTestBase.exampleRule;

class NavigationRuleTest {

    @Test
    void copyIsDeep() {
        var rule = exampleRule();
        var copy = rule.copy();

        assertEquals(rule, copy);
        assertNotSame(rule, copy);
        assertNotSame(rule.from, copy.from);
        assertNotSame(rule.to, copy.to);

        copy.from.setTag("OTHER");
        assertNotEquals(rule, copy);
    }

    @Test
    void equalsAndHashCodeAreContentBased() {
        NavigationRule a = new NavigationRule(new Xml("1", "2", "3"), new JavaAnnotation("4", "5"));
        NavigationRule b = new NavigationRule(new Xml("1", "2", "3"), new JavaAnnotation("4", "5"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(new Object(), a);
        assertNotEquals(null, a);

        assertNotEquals(a, new NavigationRule(a.from, new JavaAnnotation("4", "7")));
        assertNotEquals(a, new NavigationRule(new Xml("1", "2", "2"), b.to));
    }

    @Test
    void defaultConstructorMakesEmptyRule() {
        NavigationRule rule = new NavigationRule();
        assertEquals(new Xml(), rule.from);
        assertEquals(new JavaAnnotation(), rule.to);
    }

    @Test
    void navigationRuleSerializes() {
        NavigationRule rule = exampleRule();

        Element xml = XmlSerializer.serialize(rule);
        NavigationRule restored = XmlSerializer.deserialize(xml, NavigationRule.class);

        assertEquals(rule, restored);
    }
}
