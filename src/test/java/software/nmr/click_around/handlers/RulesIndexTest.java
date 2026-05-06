package software.nmr.click_around.handlers;

import org.junit.jupiter.api.Test;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.handlers.RulesIndex.DRSet;
import software.nmr.click_around.settings.NavigationRule;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static software.nmr.click_around.settings.SettingsTestBase.exampleRule;

class RulesIndexTest {
    public static final JavaAnnotation A = new JavaAnnotation("c.A", "v");
    public static final JavaAnnotation B = new JavaAnnotation("c.B", "v");

    @Test
    void twoWayRuleIndexesBothDirections() {
        var rule = exampleRule();
        rule.type = NavigationRule.NavigationType.TWO_WAY;
        var index = new RulesIndex(Stream.of(rule));

        assertNotNull(index.xml.get("ns").get("src"), "forward direction should be indexed");
        assertFalse(index.javaAnnotation.isEmpty(), "reverse direction should be indexed");
        assertNotNull(index.javaAnnotation.get("val").get("com.x.Y"));
    }

    @Test
    void drSetMergeSingleWithSingle() {
        var a = DRSet.of(A, A);
        var b = DRSet.of(B, B);
        var merged = a.merge(b);
        assertEquals(2, merged.size());
    }

    @Test
    void drSetMergeDuplicateIsTolerated() {
        var a = DRSet.of(A, A);
        var b = DRSet.of(A, A);
        var merged = a.merge(b);
        assertEquals(1, merged.size());
    }

    @Test
    void drSetMergeMultipleIntoSingle() {
        var a = DRSet.of(A, A);
        var b = DRSet.of(B, B);
        var c = DRSet.of(A, B);
        a.merge(b);
        a.merge(c);
        assertEquals(3, a.size());
    }

    @Test
    void drSetMergeSingleIntoMulti() {
        var a = DRSet.of(A, A);
        var b = DRSet.of(B, B);
        var c = DRSet.of(A, B);
        // Make b have size > 1 first
        b.merge(c);
        // Now merge b (multi) into a (single)
        a.merge(b);
        assertEquals(3, a.size());
    }

    @Test
    void multipleRulesForSameAnnotationMergeInIndex() {
        var r1 = new NavigationRule(A, A);
        var r2 = new NavigationRule(A, B);

        var index = new RulesIndex(Stream.of(r1, r2));

        var attrMap = index.javaAnnotation.get("value");
        assertNotNull(attrMap);
        var drSet = attrMap.get("com.X");
        assertNotNull(drSet);
        assertEquals(2, drSet.size());
    }
}
