package software.nmr.click_around.filters;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.sun.istack.Nullable;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 * Checks the element's containing file against a glob/regex pattern.
 *
 * @see java.nio.file.FileSystem#getPathMatcher(String) for syntax
 */
public class PathFilter implements JavaAnnotation.SecondaryTag, Xml.SecondaryTag {
    private static final Logger LOG = Logger.getInstance(PathFilter.class);

    private String pattern;
    private PathMatcher matcher;

    @XmlAttribute(required = true)
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.matcher = FileSystems.getDefault().getPathMatcher(pattern);
    }

    @Override
    public boolean test(PsiElement element) {
        try {
            return matches(element.getContainingFile().getVirtualFile());
        } catch (Exception e) {
            LOG.debug("PathFilter match error", e);
            return false;
        }
    }

    public boolean matches(@Nullable VirtualFile virtualFile) {
        return virtualFile != null && matcher.matches(Paths.get(virtualFile.getPath()));
    }
}
