package com.github.weisj.darkmode.platform.windows;

import com.github.weisj.darkmode.platform.ThemeMonitorService;

public class WindowsThemeMonitorService implements ThemeMonitorService {

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
        return WindowsLibrary.get().isLoaded();
    }
}
