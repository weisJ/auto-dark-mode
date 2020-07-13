package com.github.weisj.darkmode.platform.linux;

import com.github.weisj.darkmode.platform.ThemeMonitorService;

public class LinuxThemeMonitorService implements ThemeMonitorService {
    @Override
    public boolean isDarkThemeEnabled() {
        return LinuxEnvironmentDelegateHelper.isDarkThemeEnabled();
    }

    @Override
    public boolean isHighContrastEnabled() {
        return LinuxEnvironmentDelegateHelper.isHighContrastEnabled();
    }

    @Override
    public long createEventHandler(Runnable callback) {
        return LinuxEnvironmentDelegateHelper.createEventHandler(callback);
    }

    @Override
    public void deleteEventHandler(long eventHandle) {
        LinuxEnvironmentDelegateHelper.deleteEventHandler(eventHandle);
    }

    @Override
    public boolean isActive() {
        return LinuxEnvironmentDelegateHelper.isActive();
    }

    @Override
    public void uninstall() {
        LinuxEnvironmentDelegateHelper.uninstall();
    }

    @Override
    public void install() {
        LinuxEnvironmentDelegateHelper.install();
    }
}
