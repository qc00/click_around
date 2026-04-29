package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.util.xmlb.annotations.Tag;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import java.util.Objects;

@Tag("rule")
public class NavigationRule {
    public Xml from;
    public JavaAnnotation to;

    public NavigationRule() {
        this(new Xml(), new JavaAnnotation());
    }

    @VisibleForTesting
    public NavigationRule(Xml from, JavaAnnotation to) {
        this.from = from;
        this.to = to;
    }

    public NavigationRule copy() { return new NavigationRule(from.copy(), to.copy()); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NavigationRule that)) return false;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    public String toString() {
        return "NavigationRule {" + from + " -> " + to + "}";
    }
}

