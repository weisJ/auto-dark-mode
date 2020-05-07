package com.github.weisj.darkmode.platform.macos;

import com.github.weisj.darkmode.platform.LibraryInfo;
import com.github.weisj.darkmode.platform.ThemeMonitorService;

public class MacOSThemeMonitorService implements ThemeMonitorService {

    private static final boolean loaded = LibraryInfo.isMacOSMojave && MacOSNative.loadLibrary();

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
