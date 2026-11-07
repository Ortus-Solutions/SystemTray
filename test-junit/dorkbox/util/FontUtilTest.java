/*
 * Copyright 2024 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The toast/dialog text used to be drawn with a hard-coded family nobody has installed, and the JVM
 * silently substituted a serif - which is what made the notifications look wrong on macOS. These tests
 * pin down the guard that catches the substitution.
 */
public
class FontUtilTest {

    /**
     * A family no machine will have, used to force the JVM's silent substitution.
     */
    private static final String MISSING_FAMILY = "ZZNoSuchFontFamilyZZ";

    private static
    String systemUiFamily() {
        return FontUtil.parseFont(FontUtil.defaultFontSpec(12, Font.PLAIN)).getFamily();
    }

    @Test
    @DisplayName("the style and size are parsed out of a family/style/size spec")
    void parsesStyleAndSize() {
        final String family = systemUiFamily();

        final Font bold = FontUtil.parseFont(family + " BOLD 16");
        assertTrue(bold.isBold());
        assertEquals(16, bold.getSize());

        final Font italic = FontUtil.parseFont(family + " ITALIC 11");
        assertTrue(italic.isItalic());
        assertEquals(11, italic.getSize());

        final Font plain = FontUtil.parseFont(family + " PLAIN 9");
        assertFalse(plain.isBold());
        assertFalse(plain.isItalic());
        assertEquals(9, plain.getSize());
    }

    @Test
    @DisplayName("an installed family is used as-is")
    void keepsAnInstalledFamily() {
        final String family = systemUiFamily();

        assertEquals(family, FontUtil.parseFont(family + " PLAIN 12").getFamily());
        assertEquals(family, FontUtil.parseFont(FontUtil.defaultFontSpec(14, Font.BOLD)).getFamily());
    }

    @Test
    @DisplayName("an uninstalled family falls back to the system UI font instead of the JVM's serif substitution")
    void fallsBackToSystemUiFont() {
        // precondition: the JVM really does substitute something for a missing family
        assertNotEquals(MISSING_FAMILY, new Font(MISSING_FAMILY, Font.PLAIN, 12).getFamily(),
                        "this test is meaningless if the JVM keeps an unknown family name");

        final Font parsed = FontUtil.parseFont(MISSING_FAMILY + " BOLD 16");

        assertEquals(systemUiFamily(), parsed.getFamily(), "an unknown family must fall back to the system UI font");
        assertNotEquals(MISSING_FAMILY, parsed.getFamily());
        assertTrue(parsed.isBold(), "the requested style must survive the fallback");
        assertEquals(16, parsed.getSize(), "the requested size must survive the fallback");
    }

    @Test
    @DisplayName("family names containing spaces survive the round trip")
    void handlesFamiliesWithSpaces() {
        final String family = systemUiFamily();

        final Font parsed = FontUtil.parseFont(family + " BOLD 13");
        assertEquals(13, parsed.getSize());
        assertTrue(parsed.isBold());

        // "Segoe UI", "DejaVu Sans" and friends must not be truncated at the first space
        assertEquals(FontUtil.parseFont(FontUtil.defaultFontSpec(13, Font.BOLD)).getFamily(), parsed.getFamily());
    }

    @Test
    @DisplayName("a spec that cannot be parsed throws instead of returning a wrong font")
    void throwsOnUnparseableSpec() {
        assertThrows(RuntimeException.class, () -> FontUtil.parseFont("no size here"));
        assertThrows(RuntimeException.class, () -> FontUtil.parseFont("Family BOLD"));
        assertThrows(RuntimeException.class, () -> FontUtil.parseFont(""));
    }
}
