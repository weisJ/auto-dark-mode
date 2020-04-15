package com.github.weisj.darkmode.platform.macos;

import com.github.weisj.darkmode.platform.ThemeMonitorService;
import com.intellij.openapi.util.SystemInfo;

public class MacOSThemeMonitorService implements ThemeMonitorService {

    private static final boolean loaded = SystemInfo.isMacOSYosemite && MacOSNative.loadLibrary();

    @Override
    public boolean isDarkThemeEnabled() {
        return MacOSNative.isDarkThemeEnabled();
    }

    @Override
    public boolean isHighContrastEnabled() {
        return MacOSNative.isHighContrastEnabled();
    }

    @Override
    public long createEventHandler(final Runnable callback) {
        return MacOSNative.createPreferenceChangeListener(callback);
    }

    @Override
    public void deleteEventHandler(final long eventHandle) {
        MacOSNative.deletePreferenceChangeListener(eventHandle);
    }

    @Override
    public boolean isActive() {
        return loaded;
    }
}
