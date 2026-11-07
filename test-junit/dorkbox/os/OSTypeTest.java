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
package dorkbox.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OSType#get(String, String)} is what decides which native library gets loaded, so a wrong
 * answer here means the wrong .so/.dll is picked at runtime. It is pure string logic, so it can be
 * pinned down without a real machine of each platform.
 *
 * Both arguments are lower-case, as documented on the method.
 */
public
class OSTypeTest {

    @Test
    @DisplayName("linux resolves per architecture")
    void resolvesLinux() {
        assertSame(OSType.Linux64, OSType.get("linux", "amd64"));
        assertSame(OSType.Linux64, OSType.get("linux", "x86_64"));
        assertSame(OSType.Linux32, OSType.get("linux", "x86"));
        assertSame(OSType.Linux32, OSType.get("linux", "i386"));
        assertSame(OSType.LinuxArm64, OSType.get("linux", "aarch64"));
        assertSame(OSType.LinuxArm32, OSType.get("linux", "arm"));

        // the arm variants that only report a prefix
        assertSame(OSType.LinuxArm32, OSType.get("linux", "armv7l"));
        assertSame(OSType.LinuxArm64, OSType.get("linux", "armv8l"));
    }

    @Test
    @DisplayName("apple resolves per architecture, including apple silicon")
    void resolvesApple() {
        assertSame(OSType.MacOsX64, OSType.get("mac os x", "x86_64"));
        assertSame(OSType.MacOsArm, OSType.get("mac os x", "aarch64"));
        assertSame(OSType.MacOsArm, OSType.get("darwin", "aarch64"));
        assertSame(OSType.MacOsX32, OSType.get("mac os x", "i386"));
    }

    @Test
    @DisplayName("windows resolves per architecture")
    void resolvesWindows() {
        assertSame(OSType.Windows64, OSType.get("windows", "amd64"));
        assertSame(OSType.Windows32, OSType.get("windows", "x86"));
    }

    @Test
    @DisplayName("the unix-ish platforms resolve to Solaris or generic Unix")
    void resolvesUnixAndSolaris() {
        assertSame(OSType.Solaris, OSType.get("sunos", "sparc"));
        assertSame(OSType.Solaris, OSType.get("solaris", "sparc"));
        assertSame(OSType.Unix64, OSType.get("freebsd", "amd64"));
        assertSame(OSType.Unix64, OSType.get("aix", "ppc64"));
        assertSame(OSType.Unix32, OSType.get("freebsd", "x86"));
        assertSame(OSType.UnixArm, OSType.get("freebsd", "arm"));
    }

    @Test
    @DisplayName("unknown combinations resolve to null instead of guessing")
    void returnsNullForUnknown() {
        assertNull(OSType.get("plan9", "x86"), "an unknown OS family must not be guessed at");
        assertNull(OSType.get("linux", "sparc"), "an unknown linux architecture must not be guessed at");
        assertNull(OSType.get("linux", "riscv64"), "an unknown linux architecture must not be guessed at");
    }

    @Test
    @DisplayName("bitness and family helpers agree with the constants")
    void classifiesConstants() {
        assertTrue(OSType.Linux64.is64bit());
        assertTrue(OSType.LinuxArm64.is64bit());
        assertTrue(OSType.MacOsArm.is64bit());
        assertFalse(OSType.Linux64.is32bit());

        assertTrue(OSType.Linux32.is32bit());
        assertTrue(OSType.Windows32.is32bit());
        assertFalse(OSType.Linux32.is64bit());

        assertTrue(OSType.Linux64.isLinux());
        assertTrue(OSType.LinuxArm32.isLinux());
        assertFalse(OSType.Windows64.isLinux());

        assertTrue(OSType.Windows64.isWindows());
        assertTrue(OSType.MacOsX64.isMacOsX());
        assertTrue(OSType.MacOsArm.isMacOsX());
        assertTrue(OSType.Solaris.isSolaris());
        assertTrue(OSType.Unix64.isUnix());
        assertTrue(OSType.LinuxArm64.isArm());
        assertTrue(OSType.MacOsArm.isArm());
        assertTrue(OSType.AndroidX86_64.isAndroid());
    }

    @Test
    @DisplayName("every constant carries a standard name and a native library extension")
    void constantsAreFullyPopulated() {
        for (final OSType type : OSType.values()) {
            assertEquals(type.getStandardName(), type.getStandardName().toLowerCase(),
                         type + " standard names are used in file/resource names and must stay lower-case");
            assertFalse(type.getStandardName().isEmpty(), type + " has no standard name");
            assertTrue(type.getLibraryNames().length > 0, type + " has no native library extension");
        }
    }
}
