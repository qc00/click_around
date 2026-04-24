package software.nmr.click_around.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.xml.*;
import org.jetbrains.annotations.Nullable;
import software.nmr.click_around.settings.NavigationRule;
import software.nmr.click_around.settings.ProjectSettings;
import software.nmr.click_around.settings.SymbolType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class XmlToJavaGotoHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset, Editor editor) {
        if (sourceElement == null) return null;
        var project = sourceElement.getProject();

        MatchContext ctx = extractMatchContext(sourceElement);
        if (ctx == null) return null;

        var allRules = ProjectSettings.getInstance(project).getEffectiveRules();
        List<NavigationRule> matchingRules = filterRules(allRules, ctx);
        if (matchingRules.isEmpty()) return null;

        GlobalSearchScope projectScope = GlobalSearchScope.allScope(project);
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        List<PsiElement> targets = new ArrayList<>();
        for (NavigationRule rule : matchingRules) {
            PsiClass annotationClass = facade.findClass(rule.annotationFqdn, projectScope);
            if (annotationClass == null) continue;

            String propertyName = rule.getEffectiveAnnotationProperty();
            AnnotatedElementsSearch.searchPsiMembers(annotationClass, projectScope)
                    .forEach(member -> {
                        if (hasMatchingAnnotationValue(member, rule.annotationFqdn, propertyName, ctx.textValue)) {
                            targets.add(member);
                        }
                        return true;
                    });
        }

        return targets.isEmpty() ? null : targets.toArray(PsiElement.EMPTY_ARRAY);
    }

    private static @Nullable MatchContext extractMatchContext(PsiElement element) {
        if (element instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_NAME) {
            PsiElement parent = token.getParent();
            if (parent instanceof XmlTag tag) {
                return new MatchContext(tag.getLocalName(), tag.getNamespace(), SymbolType.TAG_NAME, null);
            }
        }

        if (element instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            PsiElement parent = token.getParent();
            if (parent instanceof XmlAttributeValue attrValue) {
                PsiElement grandParent = attrValue.getParent();
                if (grandParent instanceof XmlAttribute attr) {
                    XmlTag tag = attr.getParent();
                    String namespace = tag != null ? tag.getNamespace() : "";
                    String value = attrValue.getValue();
                    return new MatchContext(value, namespace, SymbolType.ANY_ATTRIBUTE_VALUE, attr.getLocalName());
                }
            }
        }

        return null;
    }

    private static List<NavigationRule> filterRules(Collection<NavigationRule> rules, MatchContext ctx) {
        List<NavigationRule> result = new ArrayList<>();
        for (NavigationRule rule : rules) {
            if (!matchesNamespace(rule.xmlNamespace, ctx.namespace)) continue;

            switch (rule.symbolType) {
                case TAG_NAME -> {
                    if (ctx.symbolType == SymbolType.TAG_NAME) result.add(rule);
                }
                case SPECIFIC_ATTRIBUTE -> {
                    if (ctx.symbolType == SymbolType.ANY_ATTRIBUTE_VALUE
                            && ctx.attributeName != null
                            && ctx.attributeName.equals(rule.attributeName)) {
                        result.add(rule);
                    }
                }
                case ANY_ATTRIBUTE_VALUE -> {
                    if (ctx.symbolType == SymbolType.ANY_ATTRIBUTE_VALUE) result.add(rule);
                }
            }
        }
        return result;
    }

    private static boolean matchesNamespace(String ruleNamespace, String actualNamespace) {
        if (ruleNamespace == null || ruleNamespace.isEmpty()) return true;
        return ruleNamespace.equals(actualNamespace);
    }

    private static boolean hasMatchingAnnotationValue(PsiModifierListOwner member,
                                                      String annotationFqdn,
                                                      String propertyName,
                                                      String expectedValue) {
        PsiModifierList modifiers = member.getModifierList();
        if (modifiers == null) return false;

        PsiAnnotation annotation = modifiers.findAnnotation(annotationFqdn);
        if (annotation == null) return false;

        PsiAnnotationMemberValue attrValue = annotation.findAttributeValue(propertyName);
        if (attrValue == null) return false;

        if (attrValue instanceof PsiLiteralExpression literal) {
            Object val = literal.getValue();
            return expectedValue.equals(val instanceof String s ? s : String.valueOf(val));
        }

        if (attrValue instanceof PsiArrayInitializerMemberValue array) {
            for (PsiAnnotationMemberValue initializer : array.getInitializers()) {
                if (initializer instanceof PsiLiteralExpression literal) {
                    Object val = literal.getValue();
                    if (expectedValue.equals(val instanceof String s ? s : String.valueOf(val))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private record MatchContext(String textValue, String namespace, SymbolType symbolType,
                                @Nullable String attributeName) {
    }
}

