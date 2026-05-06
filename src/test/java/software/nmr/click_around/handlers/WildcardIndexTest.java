package software.nmr.click_around.handlers;

import com.google.common.collect.Collections2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;
import software.nmr.click_around.handlers.RulesIndex.DRSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WildcardIndexTest {
    static final DRSet[] RULES = {
            DRSet.of(new Xml("", "tag", "x"), new JavaAnnotation("com.A", "value")),
            DRSet.of(new Xml("ns", "tag", "*"), new JavaAnnotation("com.B", "name")),
            DRSet.of(new Xml("*", "other", ""), new JavaAnnotation("com.C", "x"))};

    static final WildcardIndex<WildcardIndex<WildcardIndex<DRSet>>> INDEXED =
            RulesIndex.XML_INDEXER.index(Arrays.stream(RULES));

    static Collection<List<DRSet>> permutations() {
        return Collections2.permutations(Arrays.asList(RULES));
    }

    private final HashSet<Object> out = new HashSet<>();

    @ParameterizedTest
    @MethodSource("permutations")
    void permutationsIndexTheSame(List<DRSet> rules) {
        assertEquals(INDEXED, RulesIndex.XML_INDEXER.index(rules.stream()));
    }

    @Test
    void exactGet() {
        assertEquals(RULES[0], INDEXED.get("").get("tag").get("x"));
        assertEquals(RULES[1], INDEXED.get("ns").get("tag").get("*"));
        assertEquals(RULES[2], INDEXED.get("*").get("other").get(""));
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
            assertEquals(Set.of(RULES[1]), out);
        }));
    }

    @Test
    void emptyStringDoNotMatchWildcard() {
        INDEXED.lookup("", out::add);
        assertEquals(Set.of(INDEXED.get("")), out);
    }
}