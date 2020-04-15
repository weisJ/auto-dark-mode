package com.github.weisj.darkmode.platform;

@FunctionalInterface
public interface ThemeCallback {

    void themeChanged(final boolean isDark, final boolean isHighContrast);
}
