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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * File helpers.
 *
 * This is a plain Java replacement for the former Kotlin `FileUtil`, providing only what this project actually uses.
 */
public final class FileUtil {
    private FileUtil() {
    }

    /**
     * Recursively deletes the specified file or directory.
     *
     * @return true if the file/directory is gone when this method returns.
     */
    public static
    boolean delete(final File file) {
        if (file == null || !file.exists()) {
            return true;
        }

        final File[] children = file.listFiles();
        if (children != null) {
            for (final File child : children) {
                delete(child);
            }
        }

        return file.delete();
    }

    /**
     * Copies the source file to the destination file, creating any parent directories of the destination as necessary.
     */
    public static
    void copyFile(final File source, final File destination) throws IOException {
        if (source == null || destination == null) {
            throw new IOException("Unable to copy null file.");
        }

        final File parent = destination.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Unable to create directory: " + parent);
            }
        }

        try (InputStream inputStream = new FileInputStream(source);
             OutputStream outputStream = new FileOutputStream(destination)) {
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
    }

    /**
     * @return the extension of the specified file name (WITHOUT the leading '.'), or an empty string if there is none.
     */
    public static
    String getExtension(final String fileName) {
        if (fileName == null) {
            return "";
        }

        // the only place a '.' can be trusted is after the last path separator
        final int lastSeparator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        final int lastDot = fileName.lastIndexOf('.');

        if (lastDot <= lastSeparator || lastDot == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDot + 1);
    }

    /**
     * @return the first line of the specified file, or "" if the file cannot be read.
     */
    public static
    String readFirstLine(final File file) {
        if (file == null || !file.canRead()) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            final String line = reader.readLine();
            return line == null ? "" : line;
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * @return the contents of the specified file as a single UTF-8 string, or "" if the file cannot be read.
     */
    public static
    String readText(final File file) {
        if (file == null || !file.canRead()) {
            return "";
        }

        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
