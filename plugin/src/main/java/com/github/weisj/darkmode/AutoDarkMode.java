package com.github.weisj.darkmode;

import com.github.weisj.darkmode.platform.ThemeCallback;
import com.github.weisj.darkmode.platform.ThemeMonitor;
import com.github.weisj.darkmode.platform.ThemeMonitorService;
import com.intellij.ide.actions.QuickChangeLookAndFeel;
import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.Alarm;

import javax.swing.*;

/**
 * Automatically changes the IDEA theme based on system settings.
 */
public final class AutoDarkMode implements Disposable, ThemeCallback {
    private static final Logger LOGGER = Logger.getInstance(AutoDarkMode.class);

    private final Alarm alarm = new Alarm();
    private AutoDarkModeOptions options;
    private ThemeMonitor monitor;

    public AutoDarkMode() {
        options = ServiceManager.getService(AutoDarkModeOptions.class);
    }

    private ThemeMonitor createMonitor() {
        try {
            ThemeMonitorService service = ServiceManager.getService(ThemeMonitorService.class);
            return new ThemeMonitor(service, this);
        } catch (IllegalStateException e) {
            LOGGER.error(e);
            return null;
        }
    }

    public void start() {
        if (monitor == null) monitor = createMonitor();
        if (monitor != null) monitor.setRunning(true);
    }

    public void stop() {
        if (monitor != null) {
            monitor.setRunning(false);
            monitor = null;
        }
    }

    public void onSettingsChange() {
        if (monitor != null) monitor.requestUpdate();
    }

    @Override
    public void themeChanged(boolean isDark, boolean isHighContrast) {
        UIManager.LookAndFeelInfo target = getTargetLaf(isDark, isHighContrast);
        if (!target.equals(LafManager.getInstance().getCurrentLookAndFeel())) {
            updateLaf(target);
        }
    }

    private UIManager.LookAndFeelInfo getTargetLaf(final boolean dark, final boolean highContrast) {
        return highContrast && options.getCheckHighContrast() ? options.getHighContrastTheme()
                                                              : dark ? options.getDarkTheme()
                                                                     : options.getLightTheme();
    }

    private void updateLaf(final UIManager.LookAndFeelInfo targetLaf) {
        alarm.cancelAllRequests();
        alarm.addRequest(
            () -> QuickChangeLookAndFeel.switchLafAndUpdateUI(LafManager.getInstance(), targetLaf, false),
            Registry.get("ide.instant.theme.switch.delay").asInteger());
    }

    @Override
    public void dispose() {
        stop();
        monitor = null;
        options = null;
    }

    public void uninstall() {
        stop();
        monitor.uninstall();
    }

    public void install() {
        monitor.install();
        start();
    }
}
