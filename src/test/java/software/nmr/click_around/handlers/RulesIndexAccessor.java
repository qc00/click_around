package software.nmr.click_around.handlers;

import software.nmr.click_around.handlers.RulesIndex.AttrFqnIndex;
import software.nmr.click_around.handlers.RulesIndex.NsTagAttrIndex;

public class RulesIndexAccessor {

    public static NsTagAttrIndex getXml(RulesIndex ri) {
        return ri.xml;
    }

    public static AttrFqnIndex getJavaAnno(RulesIndex ri) {
        return ri.javaAnnotation;
    }
}
