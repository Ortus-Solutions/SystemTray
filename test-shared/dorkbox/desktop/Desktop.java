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
package dorkbox.desktop;

import java.awt.Desktop.Action;
import java.io.File;
import java.net.URI;

/**
 * Minimal replacement for the `dorkbox:Desktop` library, used ONLY by the test/example programs.
 *
 * The original library is large and pulls in extra dependencies, and the examples only ever need to open a URL or a
 * directory, so this provides just those two operations on top of java.awt.Desktop.
 */
public final class Desktop {
    private Desktop() {
    }

    public static
    void browseURL(final String url) throws java.io.IOException {
        if (url == null) {
            return;
        }

        try {
            final java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(Action.BROWSE)) {
                desktop.browse(URI.create(url));
                return;
            }
        } catch (Throwable ignored) {
        }

        browseWithSystemTool(url);
    }

    public static
    void browseDirectory(final String directory) throws java.io.IOException {
        if (directory == null) {
            return;
        }

        try {
            final File file = new File(directory).getAbsoluteFile();
            final java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(Action.OPEN)) {
                desktop.open(file);
                return;
            }
        } catch (Throwable ignored) {
        }

        browseWithSystemTool(new File(directory).getAbsolutePath());
    }

    private static
    void browseWithSystemTool(final String target) {
        try {
            final String osName = System.getProperty("os.name", "").toLowerCase();
            final ProcessBuilder builder;
            if (osName.contains("win")) {
                builder = new ProcessBuilder("cmd", "/c", "start", "", target);
            } else if (osName.contains("mac")) {
                builder = new ProcessBuilder("open", target);
            } else {
                builder = new ProcessBuilder("xdg-open", target);
            }

            builder.start();
        } catch (Throwable ignored) {
            System.err.println("Unable to browse to: " + target);
        }
    }
}
