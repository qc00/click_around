package software.nmr.click_around.settings;

import com.google.common.annotations.VisibleForTesting;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import org.jetbrains.annotations.NotNull;
import software.nmr.click_around.filters.JavaAnnotation;
import software.nmr.click_around.filters.Primary;
import software.nmr.click_around.filters.Xml;

import java.util.Objects;
import java.util.Set;

/**
 * Matches a set of {@link #definition}s against a set of {@link #usage}s, with the relationship between them specified
 * in {@link #type}.
 */
public class NavigationRule {

    public enum NavigationType {
        /**
         * One way jump from usage to definition. Does not contribute to definition's "find usage".
         * <p>
         * Note: if this plug-in found a match, the IDE will not look for other jump destinations (built-in or from
         * other plug-ins).
         */
        TO_DEFINITION("->"),
        /**
         * Jump back and forth between usage and definition. Same caveats as the one-way {@link #TO_DEFINITION}.
         */
        TWO_WAY("<->"),
        /**
         * Uses a different API from the other two, which {@code usage}
         */
        USAGE_CONTRIBUTION("uses");

        public final String indicator;

        NavigationType(String indicator) {
            this.indicator = indicator;
        }
    }

    /** Pick one of the candidate values to see its documentation */
    @XmlAttribute(required = true)
    public NavigationType type = NavigationType.TO_DEFINITION;

    @XmlElements({
            @XmlElement(name = "JavaAnnotation", type = JavaAnnotation.class, required = true),
            @XmlElement(name = "Xml", type = Xml.class, required = true),
    })
    @XmlElementWrapper(required = true)
    public Set<Primary> definition;

    @XmlElements({
            @XmlElement(name = "JavaAnnotation", type = JavaAnnotation.class, required = true),
            @XmlElement(name = "Xml", type = Xml.class, required = true),
    })
    @XmlElementWrapper(required = true)
    public Set<Primary> usage;

    @SuppressWarnings("unused")
    private NavigationRule() {
    }

    @VisibleForTesting
    public NavigationRule(@NotNull Primary usage, @NotNull Primary definition) {
        this(Set.of(usage), Set.of(definition));
    }

    @VisibleForTesting
    public NavigationRule(@NotNull Set<Primary> usage, @NotNull Set<Primary> definition) {
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
        return "NavigationRule {" + usage + " " + type.indicator + " " + definition + "}";
    }
}

