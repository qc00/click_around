package software.nmr.click_around.filters;

import com.intellij.psi.PsiElement;

import java.util.function.Predicate;

/**
 * Only used to filter the output of {@link Primary} filters. Typically, because these filters are too coarse.
 */
public interface Secondary extends Predicate<PsiElement> {
}
