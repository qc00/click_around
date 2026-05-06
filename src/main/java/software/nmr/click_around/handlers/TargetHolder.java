package software.nmr.click_around.handlers;

import com.intellij.psi.PsiElement;
import software.nmr.click_around.filters.Primary;

import java.util.HashSet;
import java.util.Set;

/**
 * Parameter & return values object
 */
public class TargetHolder {
    public PsiElement element;
    public String text;
    public Set<Primary> targets;

    public TargetHolder(PsiElement element) {
        this.element = element;
    }

    public void addPotentialMatch(RulesIndex.DRSet drSet) {
        assert text != null;
        for (var rule: drSet) {
            if (rule.from().matchAllSecondaries(element)) {
                if (targets == null) {
                    targets = rule.to();
                } else if (!(targets instanceof HashSet)) {
                    targets = new HashSet<>(targets);
                }
                targets.addAll(rule.to());
            }
        }
    }
}
