package com.github.weisj.darkmode.platform;

import com.intellij.openapi.diagnostic.Logger;

public class ThemeMonitor {

    private static final Logger LOGGER = Logger.getInstance(ThemeMonitor.class);

    private final ThemeCallback onThemeChange;
    private final ThemeMonitorService monitorService;

    private boolean dark;
    private boolean highContrast;

    private long listenerHandle;
    private boolean running;

    public ThemeMonitor(final ThemeMonitorService monitorService, final ThemeCallback callback) {
        this.monitorService = monitorService;
        this.onThemeChange = callback;
        if (monitorService == null || !monitorService.isActive()) {
            throw new IllegalStateException("Could not load library.");
        }
    }

    private void onNotification() {
        boolean newDark = monitorService.isDarkThemeEnabled();
        boolean newHighContrast = monitorService.isHighContrastEnabled();
        boolean hasChanged = highContrast != newHighContrast
                             || (!newHighContrast && dark != newDark);
        if (hasChanged) {
            dark = newDark;
            highContrast = newHighContrast;
            onThemeChange.themeChanged(dark, highContrast);
        }
    }

    public void requestUpdate() {
        onThemeChange.themeChanged(dark, highContrast);
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
        monitorService.dispose();
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
}
