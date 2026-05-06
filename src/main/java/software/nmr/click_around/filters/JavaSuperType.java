package software.nmr.click_around.filters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.InheritanceUtil;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.util.Set;

/**
 * Checks the containing class is {@code instanceof} at least one class name in {@link #fqns}.
 */
public class JavaSuperType implements JavaAnnotation.SecondaryTag {

    @XmlAttribute(required = true) public Set<String> fqns;

    @Override
    public boolean test(PsiElement element) {
        while (!(element instanceof PsiClass)) {
            if (element == null || element instanceof PsiFile) return false;
            element = element.getParent();
        }
        for (String fqn: fqns) {
            if (InheritanceUtil.isInheritor((PsiClass) element, fqn)) return true;
        }
        return false;
    }
}
