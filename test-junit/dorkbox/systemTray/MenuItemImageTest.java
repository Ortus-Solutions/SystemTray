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
package dorkbox.systemTray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dorkbox.systemTray.peer.MenuItemPeer;
import dorkbox.systemTray.util.ImageResizeUtil;
import dorkbox.systemTray.util.SizeAndScaling;
import dorkbox.util.CacheUtil;

/**
 * A caller hands the library an image stream, and that stream is not read until the entry is bound. Callers
 * close their own stream as soon as the constructor returns, which is the bug this guards: without a copy of
 * the bytes, every menu image came out as the error icon.
 */
public
class MenuItemImageTest {

    /**
     * The size the library resizes a menu image to. The real value comes from
     * {@link SizeAndScaling#initSizes}, which needs a running tray, so it is pinned here.
     */
    private static final int MENU_IMAGE_SIZE = 32;

    /**
     * Stands in for the platform implementation. The library only reads the image when the entry is bound,
     * which is the moment the caller's stream has already been closed.
     */
    private static
    class RecordingMenuItemPeer implements MenuItemPeer {
        @Override
        public
        void remove() {
        }

        @Override
        public
        void setImage(final MenuItem menuItem) {
        }

        @Override
        public
        void setEnabled(final MenuItem menuItem) {
        }

        @Override
        public
        void setText(final MenuItem menuItem) {
        }

        @Override
        public
        void setCallback(final MenuItem menuItem) {
        }

        @Override
        public
        void setShortcut(final MenuItem menuItem) {
        }

        @Override
        public
        void setTooltip(final MenuItem menuItem) {
        }
    }

    @BeforeAll
    static
    void pinMenuImageSize() {
        // Without a tray this is 0, and a resize to 0 fails - which would serve the error icon for every image
        // and make the assertions below pass for the wrong reason.
        SizeAndScaling.TRAY_MENU_SIZE = MENU_IMAGE_SIZE;
    }

    @Test
    @DisplayName("an image stream is still usable after the caller closes it")
    void imageStreamSurvivesTheCallerClosingIt(@TempDir final Path dir) throws IOException {
        final File source = writeSolidPng(dir.resolve("icon.png"));

        final MenuItem item;
        try (InputStream stream = new FileInputStream(source)) {
            item = new MenuItem("Icon", stream);
        }
        // the caller's stream is now closed, and the entry has not read it yet

        final ImageResizeUtil imageResizeUtil = new ImageResizeUtil(new CacheUtil());
        item.bind(new RecordingMenuItemPeer(), null, imageResizeUtil);

        assertTrue(item.hasImage(), "the menu entry never resolved an image");
        final File image = item.getImage();
        assertNotNull(image);
        assertTrue(image.canRead(), "the resolved image file cannot be read");

        assertNotEquals(imageResizeUtil.getErrorImage(MENU_IMAGE_SIZE).getAbsolutePath(),
                        image.getAbsolutePath(),
                        "the caller's stream had already been closed, so the error icon was used instead");

        final BufferedImage resolved = ImageIO.read(image);
        assertNotNull(resolved, "the resolved image is not a decodable image");
        assertSolid(resolved, Color.RED);
    }

    @Test
    @DisplayName("an entry with no image binds without one")
    void entryWithoutAnImageIsHarmless() {
        final MenuItem item = new MenuItem("No image");

        item.bind(new RecordingMenuItemPeer(), null, new ImageResizeUtil(new CacheUtil()));

        assertFalse(item.hasImage(), "an entry with no image must not invent one");
        assertNull(item.getImage());
    }

    @Test
    @DisplayName("an image can be replaced on an entry that is already bound")
    void imageCanBeReplacedAfterBinding(@TempDir final Path dir) throws IOException {
        final MenuItem item = new MenuItem("Icon");
        final ImageResizeUtil imageResizeUtil = new ImageResizeUtil(new CacheUtil());
        item.bind(new RecordingMenuItemPeer(), null, imageResizeUtil);

        final File source = writeSolidPng(dir.resolve("replacement.png"));
        try (InputStream stream = new FileInputStream(source)) {
            item.setImage(stream);
        }

        assertTrue(item.hasImage(), "setting an image from a stream must leave a usable image behind");
        final BufferedImage resolved = ImageIO.read(item.getImage());
        assertNotNull(resolved, "the resolved image is not a decodable image");
        assertSolid(resolved, Color.RED);
    }

    @Test
    @DisplayName("a solid PNG is written and read back unchanged")
    void writeSolidPngIsFaithful(@TempDir final Path dir) throws IOException {
        final File source = writeSolidPng(dir.resolve("icon.png"));

        final BufferedImage read = ImageIO.read(source);
        assertEquals(MENU_IMAGE_SIZE, read.getWidth());
        assertEquals(MENU_IMAGE_SIZE, read.getHeight());
        assertSolid(read, Color.RED);
    }

    private static
    File writeSolidPng(final Path path) throws IOException {
        final BufferedImage image = new BufferedImage(MENU_IMAGE_SIZE, MENU_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, MENU_IMAGE_SIZE, MENU_IMAGE_SIZE);
        } finally {
            graphics.dispose();
        }

        final File file = path.toFile();
        assertTrue(ImageIO.write(image, "png", file), "no PNG writer is available");
        return file;
    }

    /**
     * A solid fill survives a resize, so every pixel has to still be the expected colour. The error icon is
     * not a solid square, so this also fails loudly if the error icon is substituted.
     */
    private static
    void assertSolid(final BufferedImage image, final Color expected) {
        final int rgb = expected.getRGB();
        final int lastX = image.getWidth() - 1;
        final int lastY = image.getHeight() - 1;

        assertEquals(rgb, image.getRGB(0, 0), "top left pixel");
        assertEquals(rgb, image.getRGB(lastX, 0), "top right pixel");
        assertEquals(rgb, image.getRGB(0, lastY), "bottom left pixel");
        assertEquals(rgb, image.getRGB(lastX, lastY), "bottom right pixel");
        assertEquals(rgb, image.getRGB(lastX / 2, lastY / 2), "centre pixel");
    }
}
