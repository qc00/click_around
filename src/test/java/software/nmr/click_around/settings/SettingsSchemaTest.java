package software.nmr.click_around.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsSchemaTest {

    @Test
    void validateRejectsWellFormedButInvalidXml() {
        assertThrows(Exception.class, () ->
                SettingsSchema.validate("<rules><rule><unknown/></rule></rules>"));
    }

    @Test
    void validateAcceptsMinimalValidXml() {
        assertDoesNotThrow(() -> SettingsSchema.validate("<rules/>"));
    }

    @Test
    void validateThrowsOnMalformedXml() {
        assertThrows(Exception.class, () -> SettingsSchema.validate("<unclosed"));
    }
}
