package com.github.weisj.darkmode.platform;

public interface ThemeMonitorService {

    boolean isDarkThemeEnabled();

    boolean isHighContrastEnabled();

    long createEventHandler(final Runnable callback);

    void deleteEventHandler(final long eventHandle);

    boolean isActive();
}
