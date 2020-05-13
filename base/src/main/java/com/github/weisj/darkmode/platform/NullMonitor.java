package com.github.weisj.darkmode.platform;

public class NullMonitor implements ThemeMonitor {

    @Override
    public void requestUpdate() {
    }

    @Override
    public void setRunning(boolean running) {
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void install() {
    }

    @Override
    public void uninstall() {
    }
}
