package software.nmr.click_around.settings;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileDescription;
import software.nmr.click_around.settings.RulesDomFileDescription.Rules;

import java.util.List;

public class RulesDomFileDescription extends DomFileDescription<Rules> {

    public static final String NS = "https://github.com/qc00/click_around";
    public static final String ROOT_TAG_NAME = "rules";

    public RulesDomFileDescription() {
        super(Rules.class, ROOT_TAG_NAME, NS);
    }

    public interface Rules extends DomElement {
        List<NavigationRule> getRules();
    }
}
