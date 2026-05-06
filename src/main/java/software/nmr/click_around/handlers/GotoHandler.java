package software.nmr.click_around.handlers;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import software.nmr.click_around.filters.Xml;

import java.util.ArrayList;
import java.util.List;

public class GotoHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset, Editor editor) {
        if (sourceElement == null) return null;
        var holder = RulesIndex.lazyGetNavigationTargets(sourceElement);

        if (holder != null) {
            var project = sourceElement.getProject();
            List<PsiElement> out = new ArrayList<>();
            List<Xml> xmlTargets = new ArrayList<>();
            for (var target: holder.targets) {
                if (target instanceof Xml xml) {
                    xmlTargets.add(xml);
                    continue;
                }
                target.search(project, holder.text, out::add);
            }
            if (!xmlTargets.isEmpty()) {
                Xml.search(project, holder.text, xmlTargets, out::add);
            }
            if (!out.isEmpty()) {
                return out.toArray(new PsiElement[0]);
            }
        }

        return null;
    }
}

