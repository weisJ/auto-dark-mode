package com.github.weisj.darkmode;

import com.github.weisj.darkmode.platform.*;
import com.intellij.ide.actions.QuickChangeLookAndFeel;
import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.Alarm;

import javax.swing.*;
import java.util.Optional;

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
            return new AbstractThemeMonitor(service, this);
        } catch (IllegalStateException e) {
            LOGGER.error(e);
            return new NullMonitor();
        }
    }

    public void start() {
        Optional.ofNullable(monitor)
                .orElseGet(this::createMonitor)
                .setRunning(true);
    }

    public void stop() {
        stop(true);
    }

    private void stop(boolean dispose) {
        Optional.ofNullable(monitor)
                .ifPresent(m -> m.setRunning(false));
        if (dispose) setNull();
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
        stop(false);
        setNull();
    }

    private void setNull() {
        monitor = null;
        options = null;
    }

    public void uninstall() {
        stop(false);
        Optional.ofNullable(monitor).ifPresent(ThemeMonitor::uninstall);
        setNull();
    }

    public void install() {
        Optional.ofNullable(monitor)
                .orElseGet(this::createMonitor)
                .install();
        start();
    }
}
