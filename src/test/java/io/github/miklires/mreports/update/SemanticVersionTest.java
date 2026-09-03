package io.github.miklires.mreports.update;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {
    @Test void comparesNumericParts() {
        assertTrue(SemanticVersion.parse("1.10.0").compareTo(SemanticVersion.parse("1.9.9")) > 0);
        assertEquals(0, SemanticVersion.parse("v1.1").compareTo(SemanticVersion.parse("1.1.0")));
    }
    @Test void rejectsInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("latest"));
    }
}
