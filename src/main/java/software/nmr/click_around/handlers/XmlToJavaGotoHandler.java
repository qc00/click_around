package software.nmr.click_around.handlers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.xml.*;
import org.jetbrains.annotations.Nullable;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.settings.ProjectSettings;
import software.nmr.click_around.settings.WildcardIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class XmlToJavaGotoHandler implements GotoDeclarationHandler {
    private static class TargetHolder {
        String text;
        final HashSet<JavaAnnotation> locations = new HashSet<>();

        WildcardIndex<WildcardIndex<WildcardIndex<JavaAnnotation>>> prepForPotentialMatch(PsiElement element, String text) {
            this.text = text;
            locations.clear();
            return ProjectSettings.getInstance(element.getProject()).getIndexByXml();
        }
    }

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset, Editor editor) {
        if (sourceElement == null) return null;
        var project = sourceElement.getProject();

        var target = new TargetHolder();
        computeWantedAnnotations(sourceElement, target);
        if (target.locations.isEmpty()) return null;

        GlobalSearchScope projectScope = GlobalSearchScope.allScope(project);
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        List<PsiElement> out = new ArrayList<>();
        for (var location : target.locations) {
            PsiClass annotationClass = facade.findClass(location.getFqn(), projectScope);
            if (annotationClass != null) {
                AnnotatedElementsSearch.searchElements(annotationClass, projectScope, PsiJvmModifiersOwner.class)
                        .forEach(member -> {
                            if (hasMatchingAnnotationValue(member, location, target.text)) {
                                out.add(member);
                            }
                            return true;
                        });
            }

        }

        return out.isEmpty() ? null : out.toArray(PsiElement.EMPTY_ARRAY);
    }

    private void computeWantedAnnotations(PsiElement element, TargetHolder target) {
        if (element instanceof XmlToken token) {
            if (token.getTokenType() == XmlTokenType.XML_NAME) {
                if (token.getParent() instanceof XmlTag tag) {
                    var indexed = target.prepForPotentialMatch(element, token.getText());
                    indexed.lookup(tag.getNamespace(), l1 -> l1.lookup(tag.getLocalName(), l2 -> l2.lookup("", target.locations::add)));
                }
            } else if (token.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                    && token.getParent() instanceof XmlAttributeValue attrValue
                    && attrValue.getParent() instanceof XmlAttribute attr) {
                XmlTag tag = attr.getParent();
                String namespace = attr.isNamespaceDeclaration() ? attr.getNamespace() : tag.getNamespace();

                var indexed = target.prepForPotentialMatch(element, attrValue.getValue());
                indexed.lookup(namespace, l1 -> l1.lookup(tag.getLocalName(), l2 -> l2.lookup(attr.getLocalName(), target.locations::add)));
            }
        }
    }

    private static boolean hasMatchingAnnotationValue(PsiModifierListOwner member,
                                                      JavaAnnotation location,
                                                      String expectedValue) {
        PsiAnnotation annotation = member.getAnnotation(location.getFqn());
        if (annotation == null) return false;

        PsiAnnotationMemberValue attrValue = annotation.findAttributeValue(location.getAttr());
        switch (attrValue) {
            case PsiLiteralExpression literal -> {
                return expectedValue.equals(String.valueOf(literal.getValue()));
            }
            case PsiArrayInitializerMemberValue array -> {
                for (PsiAnnotationMemberValue initializer : array.getInitializers()) {
                    if (initializer instanceof PsiLiteralExpression literal && expectedValue.equals(String.valueOf(literal.getValue()))) {
                        return true;
                    }
                }
            }
            case null, default -> {
            }
        }

        return false;
    }

}

