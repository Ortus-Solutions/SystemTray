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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * OS and desktop-environment detection, as plain Java.
 *
 * This replaces the former Kotlin `dorkbox:OS` dependency, so that this project no longer requires the Kotlin runtime.
 * The public shape is intentionally identical to the Kotlin original (`OS.INSTANCE.isWindows()`, `OS.Linux.INSTANCE.isFedora()`,
 * ...) so that callers did not have to change.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class OS {
    public static final String version = "1.11";

    public static final OS INSTANCE = new OS();

    public static final String LINE_SEPARATOR_UNIX = "\n";
    public static final String LINE_SEPARATOR_MACOS = "\r";
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";

    public static final String LINE_SEPARATOR = System.getProperty("line.separator", LINE_SEPARATOR_UNIX);
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir", "temp")).getAbsoluteFile();

    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private volatile OSType type = null;

    static {
        if (!TEMP_DIR.isDirectory()) {
            // create the temp dir if necessary because the TEMP dir doesn't exist.
            //noinspection ResultOfMethodCallIgnored
            TEMP_DIR.mkdirs();
        }
    }


    protected OS() {
    }

    /**
     * @return the currently running MAJOR java version as a NUMBER. For example, "Java version 1.7u45" becomes 7, and JEP 223 is used for java 9+
     */
    public
    int getJavaVersion() {
        // We are >= java 10, use JEP 223 to get the version (early releases of 9 might not have JEP 223, so 10 is guaranteed to have it)
        String fullJavaVersion = System.getProperty("java.version", "9");
        if (fullJavaVersion.startsWith("1.")) {
            switch (fullJavaVersion.charAt(2)) {
                case '4': return 4;
                case '5': return 5;
                case '6': return 6;
                case '7': return 7;
                case '8': return 8;
                case '9': return 9;
                default: return 8;
            }
        }

        fullJavaVersion = System.getProperty("java.specification.version", "10");
        try {
            // it will ALWAYS be the major release version as an integer. See http://openjdk.java.net/jeps/223
            return Integer.parseInt(fullJavaVersion);
        } catch (Exception ignored) {
            // the last valid guess we have, since the current Java implementation, whatever it is, decided not to cooperate with JEP 223.
            return 8;
        }
    }

    /**
     * @return true if the currently running JVM is using modules (JPMS) instead of the classpath.
     */
    public
    boolean getUsesJpms() {
        try {
            // java 9+ only (reflection keeps this class usable on java 8)
            final Object module = Class.class.getMethod("getModule")
                                             .invoke(OS.class);
            return (Boolean) Class.forName("java.lang.Module")
                                  .getMethod("isNamed")
                                  .invoke(module);
        } catch (Throwable ignored) {
            // java 8, or modules are not in use
            return false;
        }
    }

    /**
     * @return the *ORIGINAL* system time zone, before (*IF*) it was changed to UTC
     */
    public
    String getOriginalTimeZone() {
        return TimeZone.getDefault()
                       .getID();
    }

    /**
     * @return the JVM reported osName, lower-cased. The default (if there is none detected) is 'linux'
     */
    public
    String getOsName() {
        return System.getProperty("os.name", "linux")
                     .toLowerCase(Locale.ROOT);
    }

    /**
     * @return the JVM reported osArch, lower-cased. The default (if there is none detected) is 'amd64'
     */
    public
    String getOsArch() {
        return System.getProperty("os.arch", "amd64")
                     .toLowerCase(Locale.ROOT);
    }

    /**
     * @return the optimum number of threads for a given task. Makes certain not to take ALL the threads, always returns at least one thread.
     */
    public
    int getOptimumNumberOfThreads() {
        return Math.max(Runtime.getRuntime()
                               .availableProcessors() - 2, 1);
    }

    /**
     * @return the determined OS type
     */
    public
    OSType getType() {
        OSType local = type;
        if (local == null) {
            local = OSType.get(getOsName(), getOsArch());
            if (local == null) {
                throw new RuntimeException("Unable to determine OS type for " + getOsName() + " " + getOsArch());
            }
            type = local;
        }
        return local;
    }

    public
    String getLINE_SEPARATOR() {
        return LINE_SEPARATOR;
    }


    /**
     * @return the value of the Java system property with the specified property, or null if it does not exist.
     */
    public
    String getProperty(final String property) {
        return System.getProperty(property, null);
    }

    /**
     * @return the value of the Java system property with the specified property, while falling back to the specified default value.
     */
    public
    String getProperty(final String property, final String defaultValue) {
        return System.getProperty(property, defaultValue);
    }

    public
    boolean getBoolean(final String property, final boolean defaultValue) {
        String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }

        value = value.trim()
                     .toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return defaultValue;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        return defaultValue;
    }

    public
    int getInt(final String property, final int defaultValue) {
        final String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public
    long getLong(final String property, final long defaultValue) {
        final String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public
    float getFloat(final String property, final float defaultValue) {
        final String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public
    double getDouble(final String property, final double defaultValue) {
        final String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * @return the System Environment property in a safe way for a given property, or null if it does not exist.
     */
    public
    Map<String, String> getEnv() {
        return System.getenv();
    }

    /**
     * @return the System Environment property in a safe way for a given property, or null if it does not exist.
     */
    public
    String getEnv(final String property) {
        return System.getenv(property);
    }

    public
    File getTEMP_DIR() {
        return TEMP_DIR;
    }

    public
    boolean is32bit() {
        return getType().is32bit();
    }

    public
    boolean is64bit() {
        return getType().is64bit();
    }

    public
    boolean isX86() {
        return getType().isX86();
    }

    public
    boolean isMips() {
        return getType().isMips();
    }

    public
    boolean isArm() {
        return getType().isArm();
    }

    public
    boolean isLinux() {
        return getType().isLinux();
    }

    public
    boolean isUnix() {
        return getType().isUnix();
    }

    public
    boolean isWindows() {
        return getType().isWindows();
    }

    public
    boolean isMacOsX() {
        return getType().isMacOsX();
    }

    public
    boolean isSolaris() {
        return getType().isSolaris();
    }

    public
    boolean isAndroid() {
        return getType().isAndroid();
    }

    /**
     * Clears/removes the property from the system properties.
     */
    public
    void clearProperty(final String property) {
        System.clearProperty(property);
    }

    /**
     * @return the previous value of the system property, or null if it did not have one.
     */
    public
    String setProperty(final String property, final String value) {
        return System.setProperty(property, value);
    }

    /**
     * @return the previous value of the system property, or the defaultValue if it did not have one.
     */
    public
    String setProperty(final String property, final String value, final String defaultValue) {
        final String previous = System.setProperty(property, value);
        return previous == null ? defaultValue : previous;
    }

    static
    String execute(final long timeoutSeconds, final String... args) {
        final Process process;
        try {
            process = new ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Thread reader = new Thread(() -> {
            final byte[] buffer = new byte[4096];
            try (InputStream inputStream = process.getInputStream()) {
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (IOException ignored) {
            }
        }, "OS-process-output");
        reader.setDaemon(true);
        reader.start();

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            } else {
                reader.join(TimeUnit.SECONDS.toMillis(1));
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            process.destroyForcibly();
        }

        return new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
    }

    static
    boolean executeStatus(final long timeoutSeconds, final String... args) {
        try {
            final Process process = new ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }


    /**
     * Windows specific checks.
     */
    public static class Windows {
        public static final Windows INSTANCE = new Windows();

        private Windows() {
        }

        /**
         * @return the {major}{minor} version of windows, ie: Windows Version 10.0.10586 -> {10}{0}
         */
        public
        int[] getVersion() {
            if (!OS.INSTANCE.isWindows()) {
                return new int[] {0, 0, 0};
            }

            try {
                final String output = execute(PROCESS_TIMEOUT_SECONDS, "cmd.exe", "/c", "ver");
                if (!output.isEmpty()) {
                    // Microsoft Windows [Version 10.0.22000.2600]
                    // 'sliced' out of the [] because we don't want to include windows product names! (like Windows 2012) in the name!
                    final int start = output.indexOf('[');
                    final int end = output.indexOf(']');
                    if (start > -1 && end > start) {
                        final String shortened = output.substring(start + 1, end);
                        String digits = null;
                        for (int i = 0; i < shortened.length(); i++) {
                            if (Character.isDigit(shortened.charAt(i))) {
                                digits = shortened.substring(i);
                                break;
                            }
                        }

                        if (digits != null) {
                            final String[] split = digits.split("\\.");
                            final int[] version = new int[3];
                            for (int i = 0; i < Math.min(3, split.length); i++) {
                                try {
                                    version[i] = Integer.parseInt(split[i].replaceAll("[^0-9]", ""));
                                } catch (Exception ignored) {
                                    version[i] = 0;
                                }
                            }
                            return version;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            return new int[] {0, 0, 0};
        }

        /**
         * @return true if this is Windows 8.1 or newer
         */
        public
        boolean isWindows8_1_plus() {
            if (!OS.INSTANCE.isWindows()) {
                return false;
            }

            final int[] version = getVersion();
            // Windows 8.1 is 6.3, Windows 10 is 10.0
            return version[0] > 6 || (version[0] == 6 && version[1] >= 3);
        }
    }


    /**
     * Linux specific checks.
     */
    public static class Linux {
        public static final Linux INSTANCE = new Linux();

        private Linux() {
        }

        /**
         * @return os release info or ""
         */
        public
        String getInfo() {
            if (!OS.INSTANCE.isLinux()) {
                return "";
            }

            String data = "";
            try {
                final List<File> releaseFiles = new LinkedList<>();
                final File dir = new File("/etc");
                if (dir.isDirectory()) {
                    final File[] list = dir.listFiles();
                    if (list != null) {
                        for (final File f : list) {
                            // looking for files like /etc/os-release
                            if (f.isFile() && f.getName()
                                                 .contains("release")) {
                                releaseFiles.add(f);
                            }
                        }
                    }
                }

                if (!releaseFiles.isEmpty()) {
                    final StringBuilder fileContents = new StringBuilder(1024);
                    for (final File releaseFile : releaseFiles) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(releaseFile))) {
                            String currentLine;
                            // NAME="Arch Linux" / PRETTY_NAME="..." / ID=arch / ID_LIKE=archlinux / ...
                            // ID is always the "key" to the distro
                            while ((currentLine = reader.readLine()) != null) {
                                fileContents.append(currentLine)
                                            .append(LINE_SEPARATOR_UNIX);
                            }
                        }
                    }
                    data = fileContents.toString();
                }
            } catch (Throwable ignored) {
            }

            return data;
        }

        private volatile String info = null;

        private
        String info() {
            String local = info;
            if (local == null) {
                local = getInfo();
                info = local;
            }
            return local;
        }

        /**
         * @param id the info ID to check, ie: ubuntu, arch, debian, etc... This is what the OS vendor uses to ID their OS.
         *
         * @return true if this OS is identified as the specified ID.
         */
        public
        boolean isReleaseType(final String id) {
            // also matches on 'DISTRIB_ID' and 'VERSION_ID'
            return info().contains("ID=" + id + "\n");
        }

        public
        boolean isArch() {
            return isReleaseType("arch");
        }

        public
        boolean isDebian() {
            return isReleaseType("debian");
        }

        public
        boolean isFedora() {
            return isReleaseType("fedora");
        }

        public
        boolean isLinuxMint() {
            return isReleaseType("linuxmint");
        }

        public
        boolean isUbuntu() {
            return isReleaseType("ubuntu");
        }

        public
        boolean isKali() {
            return isReleaseType("kali");
        }

        public
        boolean isPop() {
            return isReleaseType("pop");
        }

        public
        boolean isIgel() {
            return isReleaseType("IGEL");
        }

        public
        boolean isElementaryOS() {
            try {
                final String localInfo = info();
                // ID="elementary"  (notice the extra quotes)
                return localInfo.contains("ID=\"elementary\"\n") ||
                       localInfo.contains("ID=elementary\n") ||
                       // this is specific to eOS < 0.3.2
                       localInfo.contains("ID=\"elementary OS\"\n");
            } catch (Throwable ignored) {
                return false;
            }
        }

        public
        int getFedoraVersion() {
            if (!isFedora()) {
                return 0;
            }

            try {
                final String localInfo = info();
                if (localInfo.contains("ID=fedora\n")) {
                    // should be: VERSION_ID=23\n  or something
                    final int beginIndex = localInfo.indexOf("VERSION_ID=") + 11;
                    final int endIndex = localInfo.indexOf(LINE_SEPARATOR_UNIX, beginIndex);
                    return Integer.parseInt(localInfo.substring(beginIndex, endIndex));
                }
            } catch (Throwable ignored) {
            }

            return 0;
        }

        /**
         * @return the `DISTRIB_RELEASE` info as a String, if possible. Otherwise NULL
         */
        public
        String getDistribReleaseInfo() {
            final String releaseString = "DISTRIB_RELEASE=";
            try {
                final String localInfo = info();
                int index = localInfo.indexOf(releaseString);
                if (index > -1) {
                    index += releaseString.length();
                    final int newLine = localInfo.indexOf(LINE_SEPARATOR_UNIX, index);
                    if (newLine > index) {
                        return localInfo.substring(index, newLine);
                    }
                }
            } catch (Throwable ignored) {
            }

            return null;
        }

        public
        int[] getUbuntuVersion() {
            if (!isUbuntu()) {
                return new int[] {0, 0};
            }

            return parseDistribReleaseVersion();
        }

        public
        int[] getElementaryOSVersion() {
            // 0.1 Jupiter, 0.2 Luna, 0.3 Freya, 0.4 Loki, 5.0 Juno
            if (!isElementaryOS()) {
                return new int[] {0, 0};
            }

            return parseDistribReleaseVersion();
        }

        private
        int[] parseDistribReleaseVersion() {
            final String releaseInfo = getDistribReleaseInfo();
            if (releaseInfo == null) {
                return new int[] {0, 0};
            }

            try {
                final String[] split = releaseInfo.split("\\.");
                return new int[] {Integer.parseInt(split[0]), Integer.parseInt(split[1])};
            } catch (Throwable ignored) {
                return new int[] {0, 0};
            }
        }

        public
        boolean isWSL() {
            try {
                // looking for /proc/version
                final File file = new File("/proc/version");
                if (file.canRead()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        // Linux version 4.4.0-19041-Microsoft (Microsoft@Microsoft.com) ...
                        final String line = reader.readLine();
                        return line != null && line.contains("-Microsoft");
                    }
                }

                // reading the file didn't work for whatever reason... uname -v
                return execute(PROCESS_TIMEOUT_SECONDS, "/usr/bin/uname", "-v").contains("-Microsoft");
            } catch (Throwable ignored) {
                return false;
            }
        }

        public
        boolean isRoot() {
            // this means we are running as sudo
            boolean isSudoOrRoot = System.getenv("SUDO_USER") != null;

            if (!isSudoOrRoot) {
                try {
                    // id -u
                    isSudoOrRoot = "0".equals(execute(PROCESS_TIMEOUT_SECONDS, "/usr/bin/id", "-u"));
                } catch (Throwable ignored) {
                }
            }

            return isSudoOrRoot;
        }


        /**
         * Determines the package manager in use.
         */
        public static class PackageManager {
            public static final PackageManager INSTANCE = new PackageManager();

            public enum Type {
                APT("apt install"),
                APTGET("apt-get install"),
                YUM("yum install"),
                PACMAN("pacman -S ");

                private final String installString;

                Type(final String installString) {
                    this.installString = installString;
                }

                public
                String getInstallString() {
                    return installString;
                }
            }

            private PackageManager() {
            }

            public
            Type getType() {
                if (new File("/usr/bin/apt").canExecute()) {
                    return Type.APT;
                } else if (new File("/usr/bin/apt-get").canExecute()) {
                    return Type.APTGET;
                } else if (new File("/usr/bin/yum").canExecute()) {
                    return Type.YUM;
                } else if (new File("/usr/bin/pacman").canExecute()) {
                    return Type.PACMAN;
                }

                // default is apt-get, even if it isn't correct
                return Type.APTGET;
            }

            /**
             * @return true if the package is installed
             */
            public
            boolean isPackageInstalled(final String packageName) {
                // dpkg -L libappindicator3
                // dpkg-query: package 'libappindicator3' is not installed
                if (new File("/usr/bin/dpkg").canExecute()) {
                    return !execute(PROCESS_TIMEOUT_SECONDS, "/usr/bin/dpkg", "-L", packageName).contains("is not installed");
                }

                // rpm -q libappindicator234
                // package libappindicator234 is not installed
                if (new File("/usr/bin/rpm").canExecute()) {
                    return !execute(PROCESS_TIMEOUT_SECONDS, "/usr/bin/rpm", "-q", packageName).contains("is not installed");
                }

                // pacman -Qi <packageName> -- use the exit code, 0 the package exists, 1 it doesn't
                if (new File("/usr/bin/pacman").canExecute()) {
                    return executeStatus(PROCESS_TIMEOUT_SECONDS, "/usr/bin/pacman", "-Qi", packageName);
                }

                return false;
            }
        }
    }


    /**
     * Desktop environment checks.
     */
    public static class DesktopEnv {
        public static final DesktopEnv INSTANCE = new DesktopEnv();

        public enum Env {
            Gnome, KDE, Unity, Unity7, XFCE, LXDE, MATE, Pantheon, ChromeOS, Unknown
        }

        public enum EnvType {
            X11, WAYLAND, Unknown
        }

        private DesktopEnv() {
        }

        private static
        boolean isValidCommand(final String partialExpectationInOutput, final String commandOutput) {
            return commandOutput.contains(partialExpectationInOutput) &&
                   !commandOutput.contains("not installed") &&
                   !commandOutput.contains("No such file or directory");
        }

        private
        String execute(final String... args) {
            return OS.execute(PROCESS_TIMEOUT_SECONDS, args);
        }

        public
        EnvType getType() {
            final String type = System.getenv("XDG_SESSION_TYPE");
            if ("x11".equals(type)) {
                return EnvType.X11;
            }
            if ("wayland".equals(type)) {
                return EnvType.WAYLAND;
            }
            return EnvType.Unknown;
        }

        public
        boolean isX11() {
            return getType() == EnvType.X11;
        }

        public
        boolean isWayland() {
            return getType() == EnvType.WAYLAND;
        }

        public
        boolean isMATE() {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return false;
            }

            try {
                return new File("/usr/bin/mate-about").exists();
            } catch (Throwable ignored) {
                return false;
            }
        }

        private volatile Boolean gnome = null;

        public
        boolean isGnome() {
            Boolean local = gnome;
            if (local == null) {
                local = detectGnome();
                gnome = local;
            }
            return local;
        }

        private
        boolean detectGnome() {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return false;
            }

            try {
                // note: some versions of linux can ONLY access "ps a"; FreeBSD and most linux is "ps x". We try "x" first
                boolean contains = execute("/usr/bin/ps", "x").contains("gnome-shell");
                if (!contains && OS.INSTANCE.isLinux()) {
                    // only try again if we are linux
                    contains = execute("/usr/bin/ps", "a").contains("gnome-shell");
                }
                return contains;
            } catch (Throwable ignored) {
                return false;
            }
        }

        /**
         * @return a string representing the current gnome-shell version, or NULL if it could not be found
         */
        public
        String getGnomeVersion() {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return null;
            }

            try {
                // gnome-shell --version
                final String versionString = execute("/usr/bin/gnome-shell", "--version");
                if (!versionString.isEmpty()) {
                    // GNOME Shell 3.14.1
                    final String version = versionString.replaceAll("[^\\d.]", "");
                    if (!version.isEmpty() && version.indexOf('.') > 0) {
                        // should just be 3.14.1 or 3.20 or similar
                        return version;
                    }
                }
            } catch (Throwable ignored) {
            }

            return null;
        }

        private volatile String plasmaVersion = null;
        private volatile boolean plasmaVersionChecked = false;

        /**
         * The full version number of plasma shell (if running) as a String.
         *
         * @return cannot represent '5.6.5' as a number, so we return a String instead or NULL if unknown
         */
        public
        String getPlasmaVersionFull() {
            if (!plasmaVersionChecked) {
                plasmaVersion = detectPlasmaVersion();
                plasmaVersionChecked = true;
            }
            return plasmaVersion;
        }

        private
        String detectPlasmaVersion() {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return null;
            }

            try {
                // plasmashell --version
                final String output = execute("/usr/bin/plasmashell", "--version");
                if (!output.isEmpty()) {
                    // should be: plasmashell 5.6.5   or something
                    final String s = "plasmashell ";
                    if (isValidCommand(s, output)) {
                        return output.substring(output.indexOf(s) + s.length());
                    }
                }
            } catch (Throwable ignored) {
            }

            return null;
        }

        private volatile Boolean kde = null;

        public
        boolean isKDE() {
            Boolean local = kde;
            if (local == null) {
                local = detectKde();
                kde = local;
            }
            return local;
        }

        private
        boolean detectKde() {
            final String xdg = System.getenv("XDG_CURRENT_DESKTOP");
            if (xdg == null) {
                // Check if plasmashell is running, if it is -- then we are most likely KDE
                final String plasmaVersionFull = getPlasmaVersionFull();
                return plasmaVersionFull != null && !plasmaVersionFull.startsWith("0");
            }

            return "kde".equalsIgnoreCase(xdg);
        }

        private volatile Boolean xfce = null;

        public
        boolean isXfce() {
            Boolean local = xfce;
            if (local == null) {
                local = detectXfce();
                xfce = local;
            }
            return local;
        }

        private
        boolean detectXfce() {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return false;
            }

            try {
                // ps x | grep xfce
                boolean contains = execute("/usr/bin/ps", "x").contains("xfce");
                if (!contains && OS.INSTANCE.isLinux()) {
                    // only try again if we are linux
                    contains = execute("/usr/bin/ps", "a").contains("xfce");
                }
                return contains;
            } catch (Throwable ignored) {
                return false;
            }
        }

        /**
         * There are sometimes problems with nautilus (the file browser) and some GTK methods.
         */
        public
        boolean isNautilus() {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return false;
            }

            try {
                // nautilus --version
                final String output = execute("/usr/bin/nautilus", "--version");
                if (!output.isEmpty()) {
                    // should be: GNOME nautilus 3.14.3   or something
                    return isValidCommand("GNOME nautilus ", output);
                }
            } catch (Throwable ignored) {
            }

            return false;
        }

        private volatile Boolean chromeOs = null;

        public
        boolean isChromeOS() {
            Boolean local = chromeOs;
            if (local == null) {
                local = detectChromeOs();
                chromeOs = local;
            }
            return local;
        }

        private
        boolean detectChromeOs() {
            if (!OS.INSTANCE.isLinux()) {
                return false;
            }

            try {
                // ps aux | grep chromeos
                return execute("/usr/bin/ps", "aux").contains("chromeos");
            } catch (Throwable ignored) {
                return false;
            }
        }

        /**
         * @param channel which XFCE channel to query. Cannot be null
         * @param property which property (in the channel) to query. Null will list all properties in the channel
         *
         * @return the property value or "".
         */
        public
        String queryXfce(final String channel, final String property) {
            if (!OS.INSTANCE.isLinux() && !OS.INSTANCE.isUnix()) {
                return "";
            }

            try {
                // xfconf-query -c xfce4-panel -l
                final List<String> commands = new ArrayList<>();
                commands.add("/usr/bin/xfconf-query");
                commands.add("-c");
                commands.add(channel);
                if (property != null) {
                    // get property for channel
                    commands.add("-p");
                    commands.add(property);
                } else {
                    // list all properties for the channel
                    commands.add("-l");
                }

                return execute(commands.toArray(new String[0]));
            } catch (Throwable ignored) {
                return "";
            }
        }

        private volatile Env env = null;

        public
        Env getEnv() {
            Env local = env;
            if (local == null) {
                local = detectEnv();
                env = local;
            }
            return local;
        }

        private
        Env detectEnv() {
            // if we are running as ROOT, we ***WILL NOT*** have access to 'XDG_CURRENT_DESKTOP'
            //   *unless env's are preserved, but they are not guaranteed to be
            String xdg = System.getenv("XDG_CURRENT_DESKTOP");
            if (xdg == null) {
                // maybe we are running as root??? try to autodetect if we should use app indicator or gtkstatusicon
                xdg = "unknown";
            }

            // Ubuntu 17.10+ is special ... this is ubuntu:GNOME (it now uses wayland instead of x11)
            // BLEH. if gnome-shell is running, IT'S REALLY GNOME! we must ALWAYS do this check!!
            if (isGnome()) {
                xdg = "gnome";
            } else if (isKDE()) {
                // same thing with plasmashell!
                xdg = "kde";
            } else if (isXfce()) {
                // https://github.com/dorkbox/SystemTray/issues/100
                // IGEL linux doesn't say what it is... but we know it's XFCE ... EVEN THOUGH it reports X11!!
                xdg = "xfce";
            }

            if ("unity".equalsIgnoreCase(xdg)) {
                // Ubuntu Unity is a weird combination. It's "Gnome", but it's not "Gnome Shell".
                return Env.Unity;
            } else if ("unity:unity7".equalsIgnoreCase(xdg)) {
                return Env.Unity7;
            } else if ("xfce".equalsIgnoreCase(xdg)) {
                return Env.XFCE;
            } else if ("lxde".equalsIgnoreCase(xdg)) {
                return Env.LXDE;
            } else if ("kde".equalsIgnoreCase(xdg)) {
                return Env.KDE;
            } else if ("pantheon".equalsIgnoreCase(xdg)) {
                return Env.Pantheon;
            } else if ("gnome".equalsIgnoreCase(xdg)) {
                return Env.Gnome;
            } else if (isChromeOS()) {
                // maybe it's chromeOS?
                return Env.ChromeOS;
            } else if (isMATE()) {
                return Env.MATE;
            } else {
                return Env.Unknown;
            }
        }

        public
        boolean isUnity() {
            return isUnity(getEnv());
        }

        public
        boolean isUnity(final Env env) {
            return env == Env.Unity || env == Env.Unity7;
        }
    }
}
