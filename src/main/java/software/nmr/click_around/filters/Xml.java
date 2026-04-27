package software.nmr.click_around.filters;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.xmlb.annotations.Property;

@Property(style = Property.Style.ATTRIBUTE)
public class Xml extends AbsPsiFilter<String> {
    public static final Descriptor DESC = new Descriptor(Xml.class,"Xml.namespace", "Xml.tag", "Xml.attr");

    public Xml() {
        super("", "", "");
    }

    public Xml(String namespace, String tag, String attr) {
        super(namespace, tag, attr);
    }

    @Override
    public Xml copy() {
        return copyInto(new Xml());
    }

    @Override
    public Descriptor descriptor() {
        return DESC;
    }

    public String getNamespace() {
        return data[0];
    }

    public String getTag() {
        return data[1];
    }

    public String getAttr() {
        return data[2];
    }

    public void setNamespace(String namespace) {
        data[0] = namespace;
    }

    public void setTag(String tag) {
        data[1] = tag;
    }

    public void setAttr(String attr) {
        data[2] = attr;
    }

    @Override
    public ValidationInfo validateField(int fieldIndex) {
        if (fieldIndex == 1) {
            if (getTag().isEmpty()) return GENERIC_EMPTY;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Xml <%s:%s @%s>", (Object[]) data);
    }
}
