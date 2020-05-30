package com.github.weisj.darkmode.platform.macos;

import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.ThemeMonitorService;

public class MacOSThemeMonitorService implements ThemeMonitorService {

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
        return MacOSLibrary.get().isLoaded();
    }

    @Override
    public void install() {
        if (LibraryUtil.isMacOSCatalina) {
            MacOSNative.patchAppBundle();
        }
    }
}
