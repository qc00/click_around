package software.nmr.click_around.settings;

import com.intellij.DynamicBundle;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.Arrays;
import java.util.Objects;

@Tag("rule")
public class NavigationRule {
    static final String[] FIELD_NAMES = {
            "xmlNamespace", "tagName", "attributeName", "annotationFqdn", "annotationProperty"
    };

    public String xmlNamespace = "";
    public String tagName = "";
    public String attributeName = "";
    public String annotationFqdn = "";
    public String annotationProperty = "";

    public NavigationRule() {
    }

    public NavigationRule(String xmlNamespace, String tagName, String attributeName,
                          String annotationFqdn, String annotationProperty) {
        this.xmlNamespace = xmlNamespace;
        this.tagName = tagName;
        this.attributeName = attributeName;
        this.annotationFqdn = annotationFqdn;
        this.annotationProperty = annotationProperty;
    }

    public NavigationRule copy() {
        return new NavigationRule(xmlNamespace, tagName, attributeName, annotationFqdn, annotationProperty);
    }

    public ValidationInfo validateField(String fieldName) {
        switch (fieldName) {
            case "tagName":
                if (tagName.isEmpty()) return GENERIC_EMPTY;
                break;
            case "annotationFqdn":
                if (annotationFqdn.isEmpty()) return GENERIC_EMPTY;
                break;
            case "annotationProperty":
                if (annotationProperty.isEmpty()) return ANNO_PROP_EMPTY;
                break;
        }
        return null;
    }

    public String firstInvalid() {
        for (String field : FIELD_NAMES) {
            if (validateField(field) != null) return field;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavigationRule that)) return false;
        return Objects.equals(xmlNamespace, that.xmlNamespace)
                && Objects.equals(tagName, that.tagName)
                && Objects.equals(attributeName, that.attributeName)
                && Objects.equals(annotationFqdn, that.annotationFqdn)
                && Objects.equals(annotationProperty, that.annotationProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmlNamespace, tagName, attributeName, annotationFqdn, annotationProperty);
    }

    //region NLS
    private static final String BUNDLE = "messages.NavigationRule";
    private static final DynamicBundle instance = new DynamicBundle(NavigationRule.class, BUNDLE);
    private static final ValidationInfo GENERIC_EMPTY = new  ValidationInfo(localise("*.empty"));
    private static final ValidationInfo ANNO_PROP_EMPTY = new  ValidationInfo(localise("annotationProperty.empty"));

    public static @NotNull @Nls String localise(
            @NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return instance.getMessage(key, Arrays.copyOf(params, params.length));
    }
    //endregion
}

