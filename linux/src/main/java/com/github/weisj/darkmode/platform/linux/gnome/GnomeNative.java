package com.github.weisj.darkmode.platform.linux.gnome;

public class GnomeNative {

    static native String getCurrentTheme();

    static native long createEventHandler(final Runnable callback);

    static native void deleteEventHandler(final long handle);
}
