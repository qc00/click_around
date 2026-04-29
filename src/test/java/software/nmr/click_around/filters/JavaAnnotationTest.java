package software.nmr.click_around.filters;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAnnotationTest extends AbsPsiFilterContracts<JavaAnnotation> {
    {
        mandatoryColumns = Set.of(0, 1);
    }

    @Override
    protected JavaAnnotation makeValidInstance() {
        return new JavaAnnotation(SuppressWarnings.class.getName(), "value");
    }

    @Test
    void toStringContainsAllFields() {
        var s = new JavaAnnotation("com.x.Foo", "value").toString();
        assertTrue(s.contains("com.x.Foo"));
        assertTrue(s.contains("value"));
    }
}
