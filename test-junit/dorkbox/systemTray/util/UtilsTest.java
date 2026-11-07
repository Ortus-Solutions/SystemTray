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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link Utils#snapshot(InputStream)} is what makes an image stream handed to a {@code MenuItem} outlive the
 * caller's own try-with-resources block. When that copy was missing, every menu image came out as the error
 * icon because the stream had already been closed by the time the entry was bound.
 */
public
class UtilsTest {

    private static final String UTF8_TEXT = "runwar.version=1.0\nh\u00e9llo\n";

    /**
     * A stream that reports whether it was closed. {@code ByteArrayInputStream.close()} is a no-op, so the
     * real one cannot be used to prove that snapshot closed what it consumed.
     */
    private static
    class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        TrackingStream(final byte[] data) {
            super(data);
        }

        @Override
        public
        void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean wasClosed() {
            return closed;
        }
    }

    @Test
    @DisplayName("snapshot copies the bytes and closes the stream it consumed")
    void snapshotCopiesAndCloses() throws IOException {
        final byte[] data = "menu icon bytes".getBytes(StandardCharsets.UTF_8);
        final TrackingStream original = new TrackingStream(data);

        final InputStream copy = Utils.snapshot(original);

        assertNotNull(copy);
        assertNotSame(original, copy, "snapshot must not hand back the caller's stream");
        assertTrue(original.wasClosed(), "snapshot must close the stream it consumed");
        assertArrayEquals(data, copy.readAllBytes(), "the copy must hold the original bytes");
    }

    @Test
    @DisplayName("snapshot returns null for a null stream")
    void snapshotOfNullIsNull() {
        assertNull(Utils.snapshot(null));
    }

    @Test
    @DisplayName("snapshot returns null when the stream cannot be read, rather than throwing")
    void snapshotOfBrokenStreamIsNull() {
        final InputStream broken = new InputStream() {
            @Override
            public
            int read() throws IOException {
                throw new IOException("cannot read");
            }
        };

        assertNull(Utils.snapshot(broken));
    }

    @Test
    @DisplayName("toInteger parses numbers and rejects everything else")
    void toIntegerParsesOrRejects() {
        assertEquals(42, Utils.toInteger("42"));
        assertEquals(-7, Utils.toInteger("-7"));
        assertEquals(0, Utils.toInteger("0"));

        assertNull(Utils.toInteger("not a number"));
        assertNull(Utils.toInteger(""));
        assertNull(Utils.toInteger("12.5"));
        assertNull(Utils.toInteger(null));
    }

    @Test
    @DisplayName("readText and readLines decode UTF-8 and tolerate missing files")
    void readsTextAsUtf8(@TempDir final Path dir) throws IOException {
        final Path file = dir.resolve("version.properties");
        Files.write(file, UTF8_TEXT.getBytes(StandardCharsets.UTF_8));

        assertEquals(UTF8_TEXT, Utils.readText(file.toFile()), "readText must decode as UTF-8");
        assertEquals(List.of("runwar.version=1.0", "h\u00e9llo"), Utils.readLines(file.toFile()));

        final File missing = dir.resolve("missing.properties").toFile();
        assertEquals("", Utils.readText(missing), "a missing file reads as empty, not as an error");
        assertTrue(Utils.readLines(missing).isEmpty());

        assertEquals("", Utils.readText(null));
        assertTrue(Utils.readLines(null).isEmpty());
    }

    @Test
    @DisplayName("delete removes a directory tree, and tolerates null")
    void deleteIsRecursive(@TempDir final Path dir) throws IOException {
        final Path nested = dir.resolve("cache").resolve("nested");
        Files.createDirectories(nested);
        Files.write(nested.resolve("kept.txt"), "x".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(nested.resolve("kept.txt")));

        Utils.delete(dir.resolve("cache").toFile());

        assertFalse(Files.exists(dir.resolve("cache")), "delete must remove the tree, not just the leaf");
        Utils.delete(null);
    }
}
