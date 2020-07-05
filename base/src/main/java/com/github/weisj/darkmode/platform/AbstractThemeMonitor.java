package com.github.weisj.darkmode.platform;

import com.intellij.openapi.diagnostic.Logger;

public class AbstractThemeMonitor implements ThemeMonitor {

    private static final Logger LOGGER = Logger.getInstance(AbstractThemeMonitor.class);

    private ThemeCallback onThemeChange;
    private ThemeMonitorService monitorService;

    private boolean dark;
    private boolean highContrast;

    private long listenerHandle;
    private boolean running;

    public AbstractThemeMonitor(final ThemeMonitorService monitorService, final ThemeCallback callback) {
        this.monitorService = monitorService;
        this.onThemeChange = callback;
        if (monitorService == null || !monitorService.isActive()) {
            throw new IllegalStateException("Could not load library.");
        }
    }

    private void onNotification() {
        onNotification(false);
    }

    private void onNotification(final boolean forceChange) {
        boolean newDark = monitorService.isDarkThemeEnabled();
        boolean newHighContrast = monitorService.isHighContrastEnabled();
        boolean hasChanged = highContrast != newHighContrast
                             || (!newHighContrast && dark != newDark);
        if (hasChanged || forceChange) {
            dark = newDark;
            highContrast = newHighContrast;
            onThemeChange.themeChanged(dark, highContrast);
        }
    }

    public void requestUpdate() {
        onNotification(true);
    }

    protected void start() {
        dark = monitorService.isDarkThemeEnabled();
        highContrast = monitorService.isHighContrastEnabled();
        listenerHandle = monitorService.createEventHandler(this::onNotification);
        if (listenerHandle == 0) {
            LOGGER.error("Could not create notification listener. Monitoring will not be started");
            return;
        }
        running = true;
        onThemeChange.themeChanged(dark, highContrast);
        LOGGER.info("Started theme monitoring.");
    }

    protected void stop() {
        if (!running) return;
        LOGGER.info("Stopped theme monitoring.");
        running = false;
        monitorService.deleteEventHandler(listenerHandle);
    }

    public void setRunning(final boolean running) {
        if (running == isRunning()) return;
        if (running) {
            start();
        } else {
            stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void install() {
        monitorService.install();
    }

    public void uninstall() {
        monitorService.uninstall();
        onThemeChange = null;
        monitorService = null;
    }
}
