package com.github.weisj.darkmode.platform.linux;

import com.github.weisj.darkmode.platform.ThemeMonitorService;

public class NullThemeMonitorService implements ThemeMonitorService {
    @Override
    public boolean isDarkThemeEnabled() {
        return false;
    }

    @Override
    public boolean isHighContrastEnabled() {
        return false;
    }

    @Override
    public long createEventHandler(Runnable callback) {
        return 0;
    }

    @Override
    public void deleteEventHandler(long eventHandle) {

    }

    @Override
    public boolean isActive() {
        return false;
    }
}
