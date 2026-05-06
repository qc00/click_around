package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import jakarta.xml.bind.annotation.XmlRootElement;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Xml;

import java.util.Objects;

@XmlRootElement
public class NavigationRule {
    public enum NavigationType {
        TO_DEFINITION,
        TWO_WAY,
        USAGE_CONTRIBUTION
    }

    public Xml usage;
    public NavigationType type = NavigationType.TO_DEFINITION;
    public JavaAnnotation definition;

    private NavigationRule() {
    }

    @VisibleForTesting
    public NavigationRule(Xml usage, JavaAnnotation definition) {
        this.usage = usage;
        this.definition = definition;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NavigationRule that)) return false;
        return Objects.equals(usage, that.usage) && Objects.equals(definition, that.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usage, definition);
    }

    public String toString() {
        return "NavigationRule {" + usage + " -> " + definition + "}";
    }
}

