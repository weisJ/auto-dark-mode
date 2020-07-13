package com.github.weisj.darkmode.platform.linux.gnome;

import com.github.weisj.darkmode.platform.ThemeMonitorService;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.weisj.darkmode.platform.linux.gnome.GtkVariants.guessFrom;
import static java.util.Objects.isNull;

public class GnomeThemeMonitorService implements ThemeMonitorService {
    private final Logger logger = Logger.getLogger(GnomeThemeMonitorService.class.getName());
    private boolean initializedGnomeGtkThemeMonitorSuccessfully;
    private GnomeGtkThemeMonitor gnomeGtkThemeMonitor;
    private Thread gnomeGtkThemeMonitorThread;

    public GnomeThemeMonitorService() {
        try {
            gnomeGtkThemeMonitor = new GnomeGtkThemeMonitor();
            initializedGnomeGtkThemeMonitorSuccessfully = true;
        } catch (IOException | InterruptedException e) {
            initializedGnomeGtkThemeMonitorSuccessfully = false;
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public boolean isDarkThemeEnabled() {
        //TODO: Stop guessing and check the settings when available (like what is mentioned in the GtkVariants class)
        return gnomeGtkThemeMonitor.readGtkTheme().equals(guessFrom(gnomeGtkThemeMonitor.readGtkTheme()).get("night"));
    }

    @Override
    public boolean isHighContrastEnabled() {
        //TODO: This right now isn't exactly doable with the guessing implementation. It requires a user-accessible place to
        // set which theme is their "high contrast theme"
        return false;
    }

    @Override
    public long createEventHandler(Runnable callback) {
        try {
            gnomeGtkThemeMonitor.setCallback(callback);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return 0;
        }
        gnomeGtkThemeMonitorThread = new Thread(gnomeGtkThemeMonitor);
        gnomeGtkThemeMonitorThread.start();
        return 1;
    }

    @Override
    public void deleteEventHandler(long eventHandle) {
        gnomeGtkThemeMonitor.deleteEventHandler(eventHandle);
    }

    @Override
    public boolean isActive() {
        if (isNull(gnomeGtkThemeMonitorThread)) {
            //The thread will never be null unless it hasn't been initialized yet.
            //In this case, it means nothing is wrong so far and we can safely say this is active.
            return true;
        }
        if (!initializedGnomeGtkThemeMonitorSuccessfully) {
            return false;
        } else {
            return gnomeGtkThemeMonitorThread.isAlive();
        }
    }

    @Override
    public void uninstall() {

    }

    @Override
    public void install() {

    }
}
