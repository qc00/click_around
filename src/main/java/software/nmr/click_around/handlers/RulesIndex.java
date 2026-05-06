package software.nmr.click_around.handlers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ForwardingSet;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.annotations.NotNull;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Primary;
import software.nmr.click_around.filters.Xml;
import software.nmr.click_around.settings.NavigationRule;
import software.nmr.click_around.settings.ProjectSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Holds the all the {@link software.nmr.click_around.settings.NavigationRule NavigationRule}s indexed by their purpose.
 */
public class RulesIndex {
    /**
     * Internal representation for a {@link NavigationRule}, so {@code TWO_WAY} rules can use the same index.
     * @param from For the {@link Primary#getSecondaries()}
     */
    public record DirectionalRule(Primary from, Set<Primary> to) {
    }

    public static class DRSet extends ForwardingSet<DirectionalRule> {
        private @NotNull Set<DirectionalRule> actual;

        public DRSet(@NotNull DirectionalRule actual) {
            this.actual = Collections.singleton(actual);
        }

        @VisibleForTesting
        public static DRSet of(Primary from, Primary to) {
            return new DRSet(new DirectionalRule(from, Set.of(to)));
        }

        @Override
        protected @NotNull Set<DirectionalRule> delegate() {
            return actual;
        }

        public DRSet merge(DRSet other) {
            if (actual.size() == 1) {
                var myElement = actual.iterator().next();
                if (other.actual.size() == 1) {
                    try {
                        actual = Set.of(myElement, other.actual.iterator().next());
                    } catch (IllegalArgumentException e) {
                        // Rare case that both are the same: we continue to return this.
                    }
                } else {
                    actual = new HashSet<>(other.actual);
                    actual.add(myElement);
                }
            } else {
                actual = new HashSet<>(actual);
                actual.add(other.actual.iterator().next());
            }
            return this;
        }

    }

    public static class NsTagAttrIndex extends WildcardIndex<WildcardIndex<WildcardIndex<DRSet>>> {
    }

    private static Xml representative(Set<DirectionalRule> drSet) {
        return (Xml) drSet.iterator().next().from;
    }

    @VisibleForTesting
    public static final WildcardIndex.MultiLevelIndexer<DRSet, NsTagAttrIndex> XML_INDEXER =
            new WildcardIndex.MultiLevelIndexer<DRSet, DRSet>(DRSet::merge)
                    .addLevel(dr -> representative(dr).getNs())
                    .addLevel(dr -> representative(dr).getTag())
                    .addLevel(dr -> representative(dr).getAttr())
                    .withOutput(NsTagAttrIndex::new);

    public static class AttrFqnIndex extends HashMap<String, HashMap<String, DRSet>> {
    }

    @VisibleForTesting final NsTagAttrIndex xml;
    @VisibleForTesting final AttrFqnIndex javaAnnotation = new AttrFqnIndex();

    public RulesIndex(Stream<NavigationRule> rules) {
        ArrayList<DRSet> forXml = new ArrayList<>();

        rules.forEach(rule -> {
            switch (rule.type) {
                case TWO_WAY:
                    indexByType(rule.definition, rule.usage, forXml);
                    // fall-through:
                case TO_DEFINITION:
                    indexByType(rule.usage, rule.definition, forXml);
                    break;
                default:
                    throw new IllegalArgumentException("Bug: NavigationRule.type=" + rule.type);
            }
        });

        xml = XML_INDEXER.index(forXml.stream());
    }

    private void indexByType(Collection<Primary> froms, Set<Primary> tos, ArrayList<DRSet> forXml) {
        for (Primary from: froms) {
            var dr = new DRSet(new DirectionalRule(from, tos));
            if (from instanceof Xml) {
                forXml.add(dr);
            } else if (from instanceof JavaAnnotation ja) {
                javaAnnotation.computeIfAbsent(ja.getAttr(), k -> new HashMap<>()).merge(ja.getFqn(), dr, DRSet::merge);
            } else {
                throw new UnsupportedOperationException("Unknown Primary Type: " + froms.getClass());
            }
        }
    }

    //
    private static RulesIndex getIndex(PsiElement element) {
        return ProjectSettings.getInstance(element.getProject()).getCombedRules();
    }

    public static TargetHolder lazyGetNavigationTargets(PsiElement element) {
        TargetHolder holder = null;

        // Do quick check(s) before we create the index and holder, to prevent slowing down irrelevant navigation
        if (element instanceof XmlToken token) {
            Xml.getNavigationTargets(token, getIndex(element).xml, holder = new TargetHolder(element));
        } else if (element instanceof PsiJavaToken) {
            var quick = JavaAnnotation.getPsiNameValuePair(element);
            if (quick != null) {
                JavaAnnotation.getNavigationTargets(quick, getIndex(element).javaAnnotation,
                        holder = new TargetHolder(element));
            }
        }

        return holder != null && holder.targets != null ? holder : null;
    }
}
