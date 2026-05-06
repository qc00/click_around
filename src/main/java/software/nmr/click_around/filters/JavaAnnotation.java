package software.nmr.click_around.filters;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.sun.istack.Nullable;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import software.nmr.click_around.handlers.RulesIndex.AttrFqnIndex;
import software.nmr.click_around.handlers.TargetHolder;

import java.util.Set;

/**
 * Matches attribute value(s) in Java annotations.
 */
public class JavaAnnotation extends Primary {

    public interface SecondaryTag extends Secondary {
    }

    private static final Logger LOG = Logger.getInstance(JavaAnnotation.class);

    protected JavaAnnotation() {
        this(null, null);
    }

    @VisibleForTesting
    public JavaAnnotation(String fqn, String attr) {
        super(fqn, attr);
    }

    /**
     * Fully qualified name of the Java annotation to match. Required.
     */
    @XmlAttribute(required = true)
    public String getFqn() {
        return field(0);
    }

    public void setFqn(String fqn) {
        field(0, fqn);
    }

    /**
     * Annotation attribute containing the target symbol name. Use "value" for the default attribute.
     */
    @XmlAttribute(required = true)
    public String getAttr() {
        return field(1);
    }

    public void setAttr(String attr) {
        field(1, attr);
    }

    @XmlElements({
            @XmlElement(name = "path", type = PathFilter.class),
            @XmlElement(name = "super", type = JavaSuperType.class),
    })
    @SuppressWarnings("unchecked")
    @Override
    public Set<SecondaryTag> getSecondaries() {
        return (Set<SecondaryTag>) secondaries;
    }

    @Override
    public String toString() {
        return "JavaAnnotation {" + getFqn() + "." + getAttr() + "}";
    }

    //

    public static PsiNameValuePair getPsiNameValuePair(PsiElement element) {
        element = element.getParent();
        // We first need to walk out of the PsiAnnotationMemberValue (PsiExpression/ArrayInitializer/Annotation)
        for (int limit = 30; element instanceof PsiAnnotationMemberValue && limit > 0; limit--) {
            element = element.getParent();
        }
        // Also doesn't support the starting element being an annotation (or other stuff):
        return element instanceof PsiNameValuePair pair &&
                (pair.getValue() instanceof PsiExpression || pair.getValue() instanceof PsiArrayInitializerMemberValue)
                ? pair : null;
    }

    public static void getNavigationTargets(PsiNameValuePair nvp, AttrFqnIndex index, TargetHolder holder) {
        var fqns = index.get(nvp.getAttributeName());
        if (fqns == null) return;
        var grandparent = nvp.getParent().getParent();
        if (grandparent instanceof PsiAnnotation anno) {
            var target = fqns.get(anno.getQualifiedName());
            if (target != null) {
                var annoValue = getAnnoValueFromElement(nvp.getAttributeValue(), holder.element);
                if (annoValue instanceof String s) {
                    holder.text = s;
                    holder.addPotentialMatch(target);
                } else if (annoValue == null) {
                    LOG.warn("Annotation has unexpected value type: " + grandparent);
                }
            }
        } else if (warnAboutStructure) {
            LOG.warn("Psi structure of annotation not as expected: " + grandparent);
            warnAboutStructure = false;
        }
    }

    private static Object getAnnoValueFromElement(JvmAnnotationAttributeValue attributeValue, PsiElement element) {
        if (attributeValue instanceof JvmAnnotationConstantValue c) {
            return c.getConstantValue();
        } else if (attributeValue instanceof JvmAnnotationArrayValue) {
            while (!(element.getParent() instanceof PsiArrayInitializerMemberValue)) element = element.getParent();
            if (element instanceof PsiLiteralExpression literal) {
                return literal.getValue();
            } else {
                var evaluationHelper = JavaPsiFacade.getInstance(element.getProject()).getConstantEvaluationHelper();
                return evaluationHelper.computeConstantExpression(element);
            }
        }
        return null;
    }

    private static boolean warnAboutStructure = true;

    @Override
    public void search(Project project, String text, MaybeSynchronizedConsumer<PsiElement> downstream) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        // TODO: support specifying scopes
        GlobalSearchScope projectScope = GlobalSearchScope.allScope(project);

        PsiClass annotationClass = facade.findClass(getFqn(), projectScope);
        if (annotationClass != null) {
            // TODO: support changing annotation owner kind
            AnnotatedElementsSearch.searchElements(annotationClass, projectScope, PsiJvmModifiersOwner.class)
                                   .forEach(member -> {
                                       if (hasMatchingAnnotationValue(member, text) && matchAllSecondaries(member)) {
                                           downstream.accept(member);
                                       }
                                       return true;
                                   });
        }
    }

    private boolean hasMatchingAnnotationValue(PsiModifierListOwner member, String expectedValue) {
        PsiAnnotation annotation = member.getAnnotation(getFqn());
        if (annotation == null) return false;

        // Note: getAttributes() doesn't include annotation default values, but that's unlikely going to be what the
        // user wanted anyway.
        for (var attribute: annotation.getAttributes()) {
            if (attribute.getAttributeName().equals(getAttr())) {
                return isValueMatch(attribute.getAttributeValue(), expectedValue);
            }
        }

        return false;
    }

    private static boolean isValueMatch(@Nullable JvmAnnotationAttributeValue value, String expectedValue) {
        switch (value) {
            // See com.intellij.psi.PsiJvmConversionHelper.getAnnotationAttributeValue() for possible types
            case JvmAnnotationConstantValue c -> {
                var cVal = c.getConstantValue();
                return cVal instanceof String && cVal.equals(expectedValue);
            }
            case JvmAnnotationArrayValue a -> {
                for (var item: a.getValues()) {
                    if (isValueMatch(item, expectedValue)) return true;
                }
                return false;
            }
            case null, default -> {
                return false;
            }
        }
    }
}
