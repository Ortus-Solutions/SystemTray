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

/**
 * The possible OS types, and the native library extensions/naming used by each.
 *
 * This is a plain Java replacement for the former Kotlin `OSType`.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public enum OSType {
    Windows32("windows_32", ".dll"),
    Windows64("windows_64", ".dll"),
    Linux32("linux_32", ".so"),
    Linux64("linux_64", ".so"),

    MacOsX32("macosx_32", ".jnilib", ".dylib"),
    MacOsX64("macosx_64", ".jnilib", ".dylib"),
    MacOsArm("macosx_aarch64", ".jnilib", ".dylib"),
    UnixArm("unix_arm", ".so"),
    Unix32("unix_32", ".so"),

    Unix64("unix_64", ".so"),
    Solaris("solaris", ".so"),
    AndroidArm56("android_arm56", ".so"),  // 32bit no hardware float support
    AndroidArm7("android_arm7", ".so"),    // 32bit hardware float support
    AndroidArm8("android_arm8", ".so"),    // 64bit (w/ hardware float. everything now has hard float)
    AndroidMips("android_mips", ".so"),    // 32bit mips
    AndroidX86("android_x86", ".so"),      // 32bit x86 (usually emulator)
    AndroidMips64("android_mips64", ".so"),// 64bit mips
    AndroidX86_64("android_x86_64", ".so"),// 64bit x86 (usually emulator)

    /*
     * Linux OS, Hard float, meaning floats are handled in hardware. WE ONLY SUPPORT HARD FLOATS for linux ARM!.
     * For Raspberry-PI, Beaglebone, Odroid, etc PCs
     */
    LinuxArm32("linux_arm7_hf", ".so"),
    LinuxArm64("linux_arm8_hf", ".so");


    private final String standardName;
    private final String[] libraryNames;

    OSType(final String standardName, final String... libraryNames) {
        this.standardName = standardName;
        this.libraryNames = libraryNames;
    }

    public
    String getStandardName() {
        return standardName;
    }

    public
    String[] getLibraryNames() {
        return libraryNames;
    }

    public
    boolean is64bit() {
        return this == Linux64 || this == LinuxArm64 || this == Windows64 || this == MacOsX64 || this == MacOsArm ||
               this == AndroidArm8 || this == AndroidX86_64 || this == AndroidMips64 || this == Unix64;
    }

    public
    boolean is32bit() {
        return this == Linux32 || this == LinuxArm32 || this == Windows32 || this == MacOsX32 || this == AndroidArm56 ||
               this == AndroidArm7 || this == AndroidX86 || this == AndroidMips || this == UnixArm || this == Unix32;
    }

    public
    boolean isMips() {
        return this == AndroidMips || this == AndroidMips64;
    }

    /**
     * @return true if this is a "standard" x86/x64 architecture (intel/amd/etc) processor.
     */
    public
    boolean isX86() {
        return this == Linux64 || this == LinuxArm64 || this == Windows64 || this == MacOsX64 || this == Linux32 ||
               this == LinuxArm32 || this == Windows32 || this == MacOsX32 || this == Unix32 || this == Unix64 ||
               this == AndroidX86 || this == AndroidX86_64;
    }

    public
    boolean isArm() {
        return this == LinuxArm32 || this == LinuxArm64 || this == AndroidArm56 || this == AndroidArm7 || this == AndroidArm8 ||
               this == MacOsArm || this == UnixArm;
    }

    public
    boolean isLinux() {
        return this == Linux32 || this == Linux64 || this == LinuxArm64 || this == LinuxArm32;
    }

    public
    boolean isUnix() {
        return this == Unix32 || this == Unix64 || this == UnixArm;
    }

    public
    boolean isSolaris() {
        return this == Solaris;
    }

    public
    boolean isWindows() {
        return this == Windows64 || this == Windows32;
    }

    public
    boolean isMacOsX() {
        return this == MacOsX64 || this == MacOsX32 || this == MacOsArm;
    }

    public
    boolean isAndroid() {
        return this == AndroidArm56 || this == AndroidArm7 || this == AndroidX86 || this == AndroidMips ||
               this == AndroidArm8 || this == AndroidX86_64 || this == AndroidMips64;
    }

    /**
     * @return the type matching the specified osName/osArch (both lower-case), or null if it cannot be determined.
     */
    static
    OSType get(final String osName, final String osArch) {
        if (osName.startsWith("linux")) {
            // best way to determine if it's android.
            // Sometimes java binaries include Android classes on the classpath, even if it isn't actually Android, so we check the VM
            final boolean isAndroid = "Dalvik".equals(System.getProperty("java.vm.name", ""));
            if (isAndroid) {
                // android check from https://stackoverflow.com/questions/14859954/android-os-arch-output-for-arm-mips-x86
                switch (osArch) {
                    case "armeabi": return AndroidArm56;      // old/low-end non-hf 32bit cpu
                    case "armeabi-v7a": return AndroidArm7;   // 32bit hf cpu
                    case "arm64-v8a": return AndroidArm8;     // 64bit hf cpu
                    case "x86": return AndroidX86;            // 32bit x86 (usually emulator)
                    case "x86_64": return AndroidX86_64;      // 64bit x86 (usually emulator)
                    case "mips": return AndroidMips;          // 32bit mips
                    case "mips64": return AndroidMips64;      // 64bit mips
                    default: return null;
                }
            }

            switch (osArch) {
                case "i386":
                case "x86": return Linux32;
                case "arm": return LinuxArm32;
                case "x86_64":
                case "amd64": return Linux64;
                case "aarch64": return LinuxArm64;
                default:
                    // oddballs (android usually)
                    if (osArch.startsWith("arm64")) {
                        return LinuxArm64;
                    }
                    if (osArch.startsWith("arm")) {
                        return osArch.contains("v8") ? LinuxArm64 : LinuxArm32;
                    }
                    return null;
            }
        } else if (osName.startsWith("windows")) {
            return "amd64".equals(osArch) ? Windows64 : Windows32;
        } else if (osName.startsWith("mac") || osName.startsWith("darwin")) {
            switch (osArch) {
                case "x86_64": return MacOsX64;
                case "aarch64": return MacOsArm;
                default:
                    // new macOS is no longer 32 bit, but just in case.
                    return MacOsX32;
            }
        } else if (osName.startsWith("freebsd") || osName.contains("nix") || osName.contains("nux") || osName.startsWith("aix")) {
            switch (osArch) {
                case "x86":
                case "i386": return Unix32;
                case "arm": return UnixArm;
                default: return Unix64;
            }
        } else if (osName.startsWith("solaris") || osName.startsWith("sunos")) {
            return Solaris;
        }

        return null;
    }
}
