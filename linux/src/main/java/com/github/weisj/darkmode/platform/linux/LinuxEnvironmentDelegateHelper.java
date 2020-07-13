package com.github.weisj.darkmode.platform.linux;

import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.ThemeMonitorService;
import com.github.weisj.darkmode.platform.linux.gnome.GnomeThemeMonitorService;

public class LinuxEnvironmentDelegateHelper {
    private static final ThemeMonitorService delegate;

    static {
        if (LibraryUtil.isGnome) {
            delegate = new GnomeThemeMonitorService();
        } else {
            delegate = new NullThemeMonitorService();
        }
    }

    static boolean isDarkThemeEnabled() {
        return delegate.isDarkThemeEnabled();
    }

    static boolean isHighContrastEnabled() {
        return delegate.isHighContrastEnabled();
    }

    static long createEventHandler(final Runnable callback) {
        return delegate.createEventHandler(callback);
    }

    static void deleteEventHandler(final long eventHandle) {
        delegate.deleteEventHandler(eventHandle);
    }

    static boolean isActive() {
        return delegate.isActive();
    }

    static void uninstall() {
        delegate.uninstall();
    }

    static void install() {
        delegate.install();
    }
}
