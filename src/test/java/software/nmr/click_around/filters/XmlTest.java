package software.nmr.click_around.filters;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlTest extends AbsPsiFilterContracts<Xml> {
    {
        mandatoryColumns = Set.of(1);
    }

    @Override
    protected Xml makeValidInstance() {
        return new Xml("ns", "tag", "attr");
    }

    @Test
    void toStringContainsAllFields() {
        var s = makeValidInstance().toString();
        assertTrue(s.contains("ns"));
        assertTrue(s.contains("tag"));
        assertTrue(s.contains("attr"));
    }
}
