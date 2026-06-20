package software.nmr.click_around.settings;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"removal", "UnstableApiUsage"})
class LegacyDocumentationTargetFinder {

    static @Nullable Pair<@NotNull PsiElement, @Nullable PsiElement> getTarget(Project project, Editor editor,
                                                                               int offset, PsiFile psiFile) {
        var docManager = DocumentationManager.getInstance(project);
        return docManager.findTargetElementAndContext(editor, offset, psiFile);
    }
}
