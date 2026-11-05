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
package dorkbox;

import java.net.URL;

import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.os.OS;
import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import dorkbox.util.ActionHandler;

/**
 * Simple manual test app for SystemTray + Notify.
 *
 * Run: java -jar SystemTray-Test.jar
 *
 * Notifications are configured exactly like runwar configures them (dark style, 5 second hide), so what you see here is
 * what runwar users see.
 */
public
class TestSystemTray {
    private static final URL ICON_TRAY = TestSystemTray.class.getResource("gear.png");
    private static final URL ICON_SERVER = TestSystemTray.class.getResource("amenity_firestation.p.000000.32.png");
    private static final URL ICON_LOGS = TestSystemTray.class.getResource("amenity_post_box.p.39AC39.32.png");
    private static final URL ICON_ABOUT = TestSystemTray.class.getResource("accommodation_camping.glow.0092DA.32.png");
    private static final URL ICON_RUN = TestSystemTray.class.getResource("transport_bus_station.p.000000.32.png");

    private final SystemTray systemTray;

    public static
    void main(String[] args) {
        new TestSystemTray();
    }

    public
    TestSystemTray() {
        SystemTray.DEBUG = true;

        this.systemTray = SystemTray.get("SysTrayTest");
        if (systemTray == null) {
            System.err.println("Unable to load SystemTray! (not supported on this OS/desktop?)");
            System.exit(1);
        }

        systemTray.setTooltip("SystemTray test");
        systemTray.setImage(ICON_TRAY);
        systemTray.setStatus("Running");

        final Menu menu = systemTray.getMenu();

        // submenu: notifications
        final Menu notifyMenu = new Menu("Notify", ICON_LOGS);
        notifyMenu.add(new MenuItem("Information", e->notify("Information", "This is an information notification", 0)));
        notifyMenu.add(new MenuItem("Warning", e->notify("Warning", "This is a warning notification", 1)));
        notifyMenu.add(new MenuItem("Error", e->notify("Error", "This is an error notification", 2)));
        notifyMenu.add(new Separator());
        notifyMenu.add(new MenuItem("Open the docs", e->notify("Docs", "This is a plain notification", 3)));
        menu.add(notifyMenu);

        // submenu: server actions (with icons)
        final Menu serverMenu = new Menu("Server", ICON_SERVER);
        serverMenu.add(new MenuItem("Start server", ICON_RUN, e->notify("Server", "Server started", 0)));
        serverMenu.add(new MenuItem("Stop server", ICON_SERVER, e->notify("Server", "Server stopped", 1)));
        serverMenu.add(new Separator());
        serverMenu.add(new MenuItem("Restart server", ICON_RUN, e->notify("Server", "Server restarted", 0)));
        menu.add(serverMenu);

        // plain items
        menu.add(new MenuItem("Menu item 1", ICON_RUN, e->notify("Menu", "You clicked menu item 1", 0)));
        menu.add(new MenuItem("Menu item 2", e->notify("Menu", "You clicked menu item 2", 0)));

        // checkbox
        final Checkbox checkbox = new Checkbox("Checkbox", e->{
            final Checkbox source = (Checkbox) e.getSource();
            notify("Checkbox", "Checked = " + source.getChecked(), 0);
        });
        menu.add(checkbox);

        menu.add(new Separator());
        menu.add(new MenuItem("About", ICON_ABOUT, e->notify("About", "SystemTray " + SystemTray.getVersion(), 0)));

        menu.add(new Separator());
        menu.add(new MenuItem("Quit", e->{
            systemTray.shutdown();
            System.exit(0);
        }));

        printEnvironment();
    }

    /**
     * Mirrors runwar's LaunchUtil.displayMessage(): dark style, 5 second hide, fixed position.
     */
    private static
    void notify(final String title, final String text, final int type) {
        final Pos position = OS.INSTANCE.isMacOsX() ? Pos.TOP_RIGHT : Pos.BOTTOM_RIGHT;

        final Notify notify = Notify.create()
                                    .title(title)
                                    .text(text)
                                    .hideAfter(5000)
                                    .position(position)
                                    .darkStyle()
                                    .onAction(new ActionHandler<Notify>() {
                                        @Override
                                        public
                                        void handle(final Notify value) {
                                        }
                                    });

        switch (type) {
            case 1:
                notify.showWarning();
                break;
            case 2:
                notify.showError();
                break;
            case 3:
                notify.show();
                break;
            default:
                notify.showInformation();
                break;
        }
    }

    private static
    void printEnvironment() {
        System.out.println("SystemTray " + SystemTray.getVersion());
        System.out.println("OS         : " + System.getProperty("os.name") + " / " + OS.INSTANCE.getType());
        System.out.println("Java       : " + System.getProperty("java.version"));
        System.out.println("Desktop env: " + OS.DesktopEnv.INSTANCE.getEnv());

        // useful when a notification shows up in the wrong place
        try {
            final java.awt.GraphicsConfiguration gc = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                                                  .getDefaultScreenDevice()
                                                                                  .getDefaultConfiguration();
            final java.awt.Rectangle bounds = gc.getBounds();
            final java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit()
                                                           .getScreenInsets(gc);
            System.out.println("Screen     : " + bounds.width + "x" + bounds.height);
            System.out.println("Insets     : top=" + insets.top + " bottom=" + insets.bottom
                                                    + " left=" + insets.left + " right=" + insets.right);
            System.out.println("Popup Y    : " + (bounds.y + bounds.height - 87 - 20 - insets.bottom)
                                                    + " (bottom-right, height 87, margin 20)");
        } catch (Throwable ignored) {
        }
    }
}
