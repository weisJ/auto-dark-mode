package com.github.weisj.darkmode.platform.windows;

import com.github.weisj.darkmode.platform.ThemeMonitorService;
import com.intellij.openapi.util.SystemInfo;

public class WindowsThemeMonitorService implements ThemeMonitorService {

    private static final boolean loaded = SystemInfo.isWin10OrNewer && WindowsNative.loadLibrary();

    @Override
    public boolean isDarkThemeEnabled() {
        return WindowsNative.isDarkThemeEnabled();
    }

    @Override
    public boolean isHighContrastEnabled() {
        return WindowsNative.isHighContrastEnabled();
    }

    @Override
    public long createEventHandler(Runnable callback) {
        return WindowsNative.createEventHandler(callback);
    }

    @Override
    public void deleteEventHandler(long eventHandle) {
        WindowsNative.deleteEventHandler(eventHandle);
    }

    @Override
    public boolean isActive() {
        return loaded;
    }
}
