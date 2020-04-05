package com.github.weisj.darkmode;

import com.intellij.ide.actions.QuickChangeLookAndFeel;
import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;

import javax.swing.*;

/**
 * Automatically changes the IDEA theme based on windows settings.
 */
@Service
public final class AutoDarkMode implements Disposable {
    private static final Logger LOGGER = Logger.getInstance(AutoDarkMode.class);

    private final LafManager lafManager;
    private final AutoDarkModeOptions options;
    private final ThemeMonitor monitor;
    private UIManager.LookAndFeelInfo currentLaf;

    public AutoDarkMode() {
        options = ServiceManager.getService(AutoDarkModeOptions.class);
        this.lafManager = LafManager.getInstance();
        this.currentLaf = lafManager.getCurrentLookAndFeel();
        if (!SystemInfo.isWin10OrNewer) {
            LOGGER.error("Plugin only supports Windows 10 or newer");
            monitor = null;
            return;
        }
        if (!DarkModeNative.loadLibrary()) {
            LOGGER.error("Could not load library.");
            monitor = null;
            return;
        }
        monitor = new ThemeMonitor(this::onThemeChange);
    }

    public void start() {
        if (monitor != null) monitor.setRunning(true);
    }

    public void stop() {
        if (monitor != null) monitor.setRunning(false);
    }

    public void onSettingsChange() {
        if (monitor != null) monitor.requestUpdate();
    }

    public void onThemeChange(final boolean isDark, final boolean isHighContrast) {
        UIManager.LookAndFeelInfo target = getTargetLaf(isDark, isHighContrast);
        if (!target.equals(currentLaf)) {
            updateLaf(target);
            currentLaf = target;
        }
    }

    private UIManager.LookAndFeelInfo getTargetLaf(final boolean dark, final boolean highContrast) {
        return highContrast && options.getCheckHighContrast() ? options.getHighContrastTheme()
                                                              : dark ? options.getDarkTheme()
                                                                     : options.getLightTheme();
    }

    private void updateLaf(final UIManager.LookAndFeelInfo targetLaf) {
        QuickChangeLookAndFeel.switchLafAndUpdateUI(lafManager, targetLaf, true);
    }

    @Override
    public void dispose() {
        stop();
    }
}
