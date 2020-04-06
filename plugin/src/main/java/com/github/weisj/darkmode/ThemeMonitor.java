package com.github.weisj.darkmode;

import com.intellij.openapi.diagnostic.Logger;

import java.util.function.BiConsumer;

public class ThemeMonitor {
    private static final Logger LOGGER = Logger.getInstance(AutoDarkMode.class);

    private final BiConsumer<Boolean, Boolean> onThemeChange;

    private boolean dark;
    private boolean highContrast;

    private long eventHandler;
    private boolean running;

    public ThemeMonitor(BiConsumer<Boolean, Boolean> onThemeChange) {
        this.onThemeChange = onThemeChange;
    }

    private void onNotification() {
        boolean newDark = DarkModeNative.isDarkThemeEnabled();
        boolean newHighContrast = DarkModeNative.isHighContrastEnabled();
        boolean hasChanged = highContrast != newHighContrast
                             || (!newHighContrast && dark != newDark);
        if (hasChanged) {
            dark = newDark;
            highContrast = newHighContrast;
            onThemeChange.accept(dark, highContrast);
        }
    }

    private void start() {
        dark = DarkModeNative.isDarkThemeEnabled();
        highContrast = DarkModeNative.isHighContrastEnabled();
        eventHandler = DarkModeNative.createEventHandler(this::onNotification);
        if (eventHandler == 0) {
            LOGGER.error("Could not create notification listener. Monitoring will not be started");
            return;
        }
        running = true;
        onThemeChange.accept(dark, highContrast);
        LOGGER.info("Started theme monitoring.");
    }

    private void stop() {
        if (!running) return;
        LOGGER.info("Stopped theme monitoring.");
        running = false;
        DarkModeNative.deleteEventHandler(eventHandler);
    }

    public void requestUpdate() {
        onThemeChange.accept(dark, highContrast);
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
