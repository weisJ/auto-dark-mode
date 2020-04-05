package com.github.weisj.darkmode;

import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class ThemeMonitor {
    private static final Logger LOGGER = Logger.getInstance(AutoDarkMode.class);

    private final BiConsumer<Boolean, Boolean> onThemeChange;

    private boolean dark;
    private boolean highContrast;

    private AtomicBoolean running = new AtomicBoolean(false);
    private long eventHandle;

    public ThemeMonitor(BiConsumer<Boolean, Boolean> onThemeChange) {
        this.onThemeChange = onThemeChange;
    }

    private void run() {
        LOGGER.info("Started theme monitoring.");
        while (running.get() && DarkModeNative.waitThemeChange(eventHandle)) {
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
        if (running.get()) {
            LOGGER.error("Monitor encountered an error. Stopping theme monitoring.");
            running.set(false);
        } else {
            LOGGER.info("Stopped theme monitoring.");
        }
    }

    private void start() {
        dark = DarkModeNative.isDarkThemeEnabled();
        highContrast = DarkModeNative.isHighContrastEnabled();
        eventHandle = DarkModeNative.createEventHandle();
        onThemeChange.accept(dark, highContrast);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        this.running.set(true);
        Thread notificationThread = new Thread(this::run);
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    private void stop() {
        this.running.set(false);
        DarkModeNative.notifyEventHandle(eventHandle);
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
        return running.get();
    }
}
