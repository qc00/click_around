package software.nmr.click_around.settings;

import com.google.common.collect.Collections2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WildcardIndexTest {
    static final NavigationRule[] RULES = {
            new NavigationRule(new Xml("", "tag", "x"), new JavaAnnotation("com.A", "value")),
            new NavigationRule(new Xml("ns", "tag", "*"), new JavaAnnotation("com.B", "name")),
            new NavigationRule(new Xml("*", "other", ""), new JavaAnnotation("com.C", "x"))};

    static final WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>> INDEXED =
            ProjectSettings.XML_INDEXER.index(Arrays.stream(RULES));

    static Collection<List<NavigationRule>> permutations() {
        return Collections2.permutations(Arrays.asList(RULES));
    }

    private final HashSet<Object> out = new HashSet<>();

    @ParameterizedTest
    @MethodSource("permutations")
    void permutationsIndexTheSame(List<NavigationRule> rules) {
        assertEquals(INDEXED, ProjectSettings.XML_INDEXER.index(rules.stream()));
    }

    @Test
    void exactGet() {
        assertEquals(RULES[0].to, INDEXED.get("").get("tag").get("x"));
        assertEquals(RULES[1].to, INDEXED.get("ns").get("tag").get("*"));
        assertEquals(RULES[2].to, INDEXED.get("*").get("other").get(""));
    }

    @Test
    void wildcardMatch() {
        INDEXED.lookup("ns", out::add);
        assertEquals(Set.of(INDEXED.get("ns"), INDEXED.get("*")), out);

        out.clear();
        INDEXED.lookup("new", out::add);
        assertEquals(Set.of(INDEXED.get("*")), out);
    }

    @Test
    void wildcardMatchLastLevel() {
        INDEXED.lookup("ns", l1 -> l1.lookup("tag", l2 -> {
            l2.lookup("", out::add);
            assertEquals(0, out.size());

            l2.lookup("asdf", out::add);
            assertEquals(Set.of(RULES[1].to), out);
        }));
    }

    @Test
    void emptyStringDoNotMatchWildcard() {
        INDEXED.lookup("", out::add);
        assertEquals(Set.of(INDEXED.get("")), out);
    }
}