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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dorkbox.notify.Notify;
import dorkbox.systemTray.util.ImageResizeUtil;
import dorkbox.util.LocationResolver;

/**
 * Resources that only exist inside the built JAR are easy to lose - a wrong directory, a build that copies
 * them to the wrong place, or a resource filter. When that happens the tray silently falls back to the error
 * icon and the notifications render nothing, so the images are asserted against the classpath on purpose.
 *
 * These look the images up exactly the way the library does.
 */
public
class PackagedResourceTest {

    @Test
    @DisplayName("every notification dialog image is packaged and decodable")
    void notifyDialogImagesArePackaged() throws IOException {
        final List<String> imageNames = List.of(Notify.DIALOG_CONFIRM,
                                                Notify.DIALOG_ERROR,
                                                Notify.DIALOG_INFORMATION,
                                                Notify.DIALOG_WARNING);

        for (final String imageName : imageNames) {
            // the same lookup Notify itself uses
            final String resource = Notify.IMAGE_PATH + File.separatorChar + imageName;

            try (InputStream stream = LocationResolver.getResourceAsStream(resource)) {
                assertNotNull(stream, resource + " is missing from the classpath");

                final BufferedImage image = ImageIO.read(stream);
                assertNotNull(image, resource + " is not a decodable image");
                assertEquals(48, image.getWidth(), resource + " width");
                assertEquals(48, image.getHeight(), resource + " height");
            }
        }
    }

    @Test
    @DisplayName("the error icon is packaged and decodable")
    void errorIconIsPackaged() throws IOException {
        // this is the lookup ImageResizeUtil.getErrorImage() uses
        try (InputStream stream = ImageResizeUtil.class.getResourceAsStream("error_32.png")) {
            assertNotNull(stream, "dorkbox/systemTray/util/error_32.png is missing from the classpath");

            final BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "the error icon is not a decodable image");
            assertEquals(32, image.getWidth());
            assertEquals(32, image.getHeight());
        }
    }

    @Test
    @DisplayName("the native tray extension script is packaged")
    void gnomeShellExtensionIsPackaged() throws IOException {
        final String resource = "dorkbox/systemTray/gnomeShell/extension.js";

        try (InputStream stream = LocationResolver.getResourceAsStream(resource)) {
            assertNotNull(stream, resource + " is missing from the classpath");
            assertTrue(stream.readAllBytes().length > 0, resource + " is empty");
        }
    }
}
