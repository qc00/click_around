package software.nmr.click_around.filters;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.TextOccurenceProcessor;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import software.nmr.click_around.handlers.RulesIndex.NsTagAttrIndex;
import software.nmr.click_around.handlers.TargetHolder;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Matches XML element <em>name</em> or attribute <em>values</em>, i.e. currently no  to match the
 */
public class Xml extends Primary {
    public interface SecondaryTag extends Secondary {
    }

    //region Fields
    private Xml() {
        this("", null, null);
    }

    public Xml(String ns, String tag, String attr) {
        super(ns, tag, attr);
    }

    /**
     * XML namespace to match. Leave empty to match the default namespace. "*" for any namespace except the default.
     */
    public String getNs() {
        return field(0);
    }

    public void setNs(String ns) {
        field(0, ns);
    }

    /**
     * XML tag name to match. Cannot be empty. "*" for any tag.
     */
    @jakarta.xml.bind.annotation.XmlAttribute(required = true)
    public String getTag() {
        return field(1);
    }

    public void setTag(String tag) {
        field(1, tag);
    }

    /**
     * If empty, then this filter matches against the element (tag) name.
     * Otherwise, specifies the attribute name whose value will be matching. "*" for any attribute.
     */
    @jakarta.xml.bind.annotation.XmlAttribute(required = true)
    public String getAttr() {
        return field(2);
    }

    public void setAttr(String attr) {
        field(2, attr);
    }
    //endregion

    @XmlElements({
            @XmlElement(name = "path", type = PathFilter.class),
    })
    @SuppressWarnings("unchecked")
    @Override
    public Set<JavaAnnotation.SecondaryTag> getSecondaries() {
        return (Set<JavaAnnotation.SecondaryTag>) secondaries;
    }

    @Override
    public String toString() {
        return "Xml {" + getNs() + ":" + getTag() + "@" + getAttr() + "}";
    }

    //
    public static void getNavigationTargets(XmlToken token, NsTagAttrIndex indexed, TargetHolder out) {
        if (token.getTokenType() == XmlTokenType.XML_NAME) {
            if (token.getParent() instanceof XmlTag tag) {
                out.text = tag.getLocalName();
                indexed.lookup(tag.getNamespace(),
                        l1 -> l1.lookup(tag.getLocalName(), l2 -> l2.lookup("", out::addPotentialMatch)));
            }
        } else if (token.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                && token.getParent() instanceof XmlAttributeValue attrValue
                && attrValue.getParent() instanceof XmlAttribute attr
                && attr.getParent() != null) {
            out.text = attrValue.getValue();
            indexed.lookup(attr.getNamespace(), l1 -> l1.lookup(attr.getParent().getLocalName(),
                    l2 -> l2.lookup(attr.getLocalName(), out::addPotentialMatch)));
        }
    }

    // ⇅ Must logically match
    Predicate<PsiElement> buildFilter(String expectedText) {
        // Note: call cheap getters first!
        return switch (getAttr()) {
            case "" -> { // Matching on tag name
                if (getTag().equals("*")) yield e -> e instanceof XmlTag t && getNs().equals(t.getNamespace());
                if (!getTag().equals(expectedText)) {
                    yield Predicates.alwaysFalse();
                }
                yield e -> e instanceof XmlTag t && t.getLocalName().equals(getTag())
                        && getNs().equals(t.getNamespace());
            }
            case "*" -> e -> e instanceof XmlAttribute a
                    // a.getParent() can return null if the tag is invalid, so use getContext():
                    && a.getContext() instanceof XmlTag t && t.getLocalName().equals(getTag())
                    && getNs().equals(a.getNamespace())
                    && expectedText.equals(a.getValue()); // getValue() is complex and involves many allocations
            default -> e -> e instanceof XmlAttribute a
                    && a.getContext() instanceof XmlTag t && t.getLocalName().equals(getTag())
                    && a.getLocalName().equals(getAttr())
                    && expectedText.equals(a.getValue());
        };
    }

    @Override
    public void search(Project project, String text, MaybeSynchronizedConsumer<PsiElement> downstream) {
        search(project, text, List.of(this), downstream);
    }

    /**
     * Batch variant of {@link Primary#search(Project, String, MaybeSynchronizedConsumer)}
     */
    public static void search(Project project, String text, List<Xml> candidates,
                              MaybeSynchronizedConsumer<PsiElement> downstream) {
        var filters = candidates.stream().map(x -> x.buildFilter(text)).toList();
        if (filters.stream().allMatch(p -> p == (Object) Predicates.alwaysFalse())) return;

        var searchHelper = PsiSearchHelper.getInstance(project);
        var scope = GlobalSearchScope.projectScope(project);

        TextOccurenceProcessor occurrenceProcessor = (element, offsetInElement) -> {
            if (element instanceof com.intellij.psi.xml.XmlElement) {
                int limit = 5; // For tag name/attribute value, the text occurrence can only be a few levels away
                for (var ancestor = element;
                     limit > 0 && ancestor != null && !(ancestor instanceof XmlFile);
                     ancestor = ancestor.getParent(), --limit) {

                    for (int i = 0; i < filters.size(); i++) {
                        if (filters.get(i).test(ancestor) && candidates.get(i).matchAllSecondaries(element)) {
                            downstream.synchronizedAccept(ancestor);
                            break;
                        }
                    }
                }
            }
            return true;
        };

        searchHelper.processElementsWithWord(occurrenceProcessor, scope, text,
                (short) (UsageSearchContext.IN_STRINGS | UsageSearchContext.IN_FOREIGN_LANGUAGES), true);
    }
}
