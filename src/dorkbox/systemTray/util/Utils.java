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
package dorkbox.systemTray.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * File and process helpers for this library.
 *
 * This replaces the former KotlinUtils.kt (which was Kotlin) with plain Java, so that this library no longer
 * requires the Kotlin compiler or the Kotlin runtime.
 */
public final class Utils {
    private static final long PROCESS_TIMEOUT_SECONDS = 60;


    private Utils() {
    }

    /**
     * Recursively deletes the specified file or directory. Any failures are ignored.
     */
    public static
    void delete(final File file) {
        if (file == null) {
            return;
        }

        final File[] children = file.listFiles();
        if (children != null) {
            for (final File child : children) {
                delete(child);
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    /**
     * Reads the contents of the specified file as UTF-8.
     *
     * @return Always returns a list, even if the file does not exist, or there are errors reading it.
     */
    public static
    List<String> readLines(final File file) {
        if (file == null || !file.canRead()) {
            return Collections.emptyList();
        }

        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Reads the contents of the specified file as a single UTF-8 string.
     *
     * @return an empty string if the file does not exist, or there are errors reading it.
     */
    public static
    String readText(final File file) {
        if (file == null || !file.canRead()) {
            return "";
        }

        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * @return the parsed integer, or null if the string is not a valid representation of a number.
     */
    public static
    Integer toInteger(final String string) {
        if (string == null) {
            return null;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Executes the specified command, waiting for it to finish, and returns its output as trimmed UTF-8.
     *
     * A non-zero exit value is NOT treated as an error, the output is returned as-is.
     *
     * @throws RuntimeException if the process could not be started, timed out, or was interrupted.
     */
    public static
    String execute(final String... command) {
        if (command == null || command.length == 0) {
            return "";
        }

        return runProcess(new ProcessBuilder(command));
    }

    /**
     * Executes the specified command via the OS shell, waiting for it to finish.
     *
     * @throws RuntimeException if the process could not be started, timed out, or was interrupted.
     */
    public static
    String executeAsShellBlocking(final String command) {
        if (command == null || command.trim()
                                     .isEmpty()) {
            return "";
        }

        final boolean isWindows = System.getProperty("os.name", "")
                                        .toLowerCase()
                                        .contains("win");

        final ProcessBuilder builder;
        if (isWindows) {
            builder = new ProcessBuilder("cmd", "/c", command);
        } else {
            builder = new ProcessBuilder("sh", "-c", command);
        }

        return runProcess(builder);
    }

    /**
     * Reads the supplied stream fully into memory and returns a stream over that copy.
     *
     * The caller's stream is consumed and closed. This is used for images that are handed to us now but not read
     * until later (menu entries are realized when they are bound), so callers are free to close their own stream
     * immediately after passing it in.
     *
     * @return a stream over the copied bytes, or null if the supplied stream was null or could not be read.
     */
    public static
    InputStream snapshot(final InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        try (InputStream stream = inputStream) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(stream.available(), 8192));
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new ByteArrayInputStream(output.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    private static
    String runProcess(final ProcessBuilder builder) {
        final Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start process: " + builder.command(), e);
        }


        // the output stream is what we return, and it must be drained on another thread so that the timeout is
        // still honored if the process never closes it.
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Thread outputReader = new Thread(() -> {
            final byte[] buffer = new byte[4096];
            try (InputStream inputStream = process.getInputStream()) {
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (IOException ignored) {
                // nothing we can do about this, and the process output is discarded
            }
        }, "SystemTray-process-output");
        outputReader.setDaemon(true);
        outputReader.start();


        // the error stream is not captured (matching the previous behavior), but it must still be drained or the
        // process can block forever once the pipe buffer is full.
        final Thread errorReader = new Thread(() -> {
            final byte[] buffer = new byte[4096];
            try (InputStream errorStream = process.getErrorStream()) {
                while (errorStream.read(buffer) != -1) {
                    // discarded
                }
            } catch (IOException ignored) {
                // nothing we can do about this, and the process output is discarded
            }
        }, "SystemTray-process-error");
        errorReader.setDaemon(true);
        errorReader.start();


        try {
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new RuntimeException("Process timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds: " + builder.command());
            }

            // the process has exited, so the reader threads will see EOF immediately.
            outputReader.join(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            process.destroyForcibly();
            throw new RuntimeException("Interrupted while waiting for process: " + builder.command(), e);
        }

        return new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
    }
}
