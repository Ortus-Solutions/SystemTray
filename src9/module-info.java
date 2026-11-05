module dorkbox.systemtray {
    exports dorkbox.systemTray;
    exports dorkbox.systemTray.peer;
    exports dorkbox.systemTray.util;

    requires transitive org.javassist; // this is an automatic module name, and emits warnings when compiling. This is OK.
    requires transitive org.slf4j;

    requires transitive com.sun.jna;
    requires transitive com.sun.jna.platform;
}
