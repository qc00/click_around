package software.nmr.click_around.filters;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.xmlb.annotations.Property;

@Property(style = Property.Style.ATTRIBUTE)
public class JavaAnnotation extends AbsPsiFilter<String> {
    public static final Descriptor DESC = new Descriptor(JavaAnnotation.class,
            "JavaAnnotation.fqn", "JavaAnnotation.attr");

    private static final ValidationInfo ANNO_PROP_EMPTY = new ValidationInfo(localise("JavaAnnotation.attr.empty"));

    public JavaAnnotation() {
        this("", "");
    }

    @VisibleForTesting
    public JavaAnnotation(String fqn, String attr) {
        super(fqn, attr);
    }

    @Override
    public JavaAnnotation copy() {
        return copyInto(new JavaAnnotation());
    }

    @Override
    public Descriptor descriptor() {
        return DESC;
    }

    public String getFqn() {
        return data[0];
    }

    public String getAttr() {
        return data[1];
    }

    public void setFqn(String fqn) {
        data[0] = fqn;
    }

    public void setAttr(String attr) {
        data[1] = attr;
    }

    @Override
    public ValidationInfo validateField(int fieldIndex) {
        switch (fieldIndex) {
            case 0:
                if (getFqn().isEmpty()) return GENERIC_EMPTY;
                break;
            case 1:
                if (getAttr().isEmpty()) return ANNO_PROP_EMPTY;
                break;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("JavaAnnotation {%s.%s}", (Object[]) data);
    }
}
