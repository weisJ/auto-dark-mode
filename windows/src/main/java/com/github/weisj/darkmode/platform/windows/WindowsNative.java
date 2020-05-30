package com.github.weisj.darkmode.platform.windows;

public final class WindowsNative {

    static native boolean isDarkThemeEnabled();

    static native boolean isHighContrastEnabled();

    static native long createEventHandler(final Runnable callback);

    static native void deleteEventHandler(final long handle);
}
