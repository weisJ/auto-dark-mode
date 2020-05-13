package com.github.weisj.darkmode.platform;

public interface ThemeMonitor {

    void requestUpdate();

    void setRunning(final boolean running);

    boolean isRunning();

    void install();

    void uninstall();
}
