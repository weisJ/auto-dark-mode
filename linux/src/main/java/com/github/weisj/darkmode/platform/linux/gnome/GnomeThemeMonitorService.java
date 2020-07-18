package com.github.weisj.darkmode.platform.linux.gnome;

import com.github.weisj.darkmode.platform.ThemeMonitorService;

import static com.github.weisj.darkmode.platform.linux.gnome.GtkVariants.guessFrom;

public class GnomeThemeMonitorService implements ThemeMonitorService {

    @Override
    public boolean isDarkThemeEnabled() {
        //TODO: Stop guessing and check the settings when available (like what is mentioned in the GtkVariants class)
        String currentTheme = GnomeNative.getCurrentTheme();
        return currentTheme.equals(guessFrom(currentTheme).get("night"));
    }

    @Override
    public boolean isHighContrastEnabled() {
        //TODO: This right now isn't exactly doable with the guessing implementation. It requires a user-accessible place to
        // set which theme is their "high contrast theme"
        return false;
    }

    @Override
    public long createEventHandler(Runnable callback) {
        return GnomeNative.createEventHandler(callback);
    }

    @Override
    public void deleteEventHandler(long eventHandle) {
        GnomeNative.deleteEventHandler(eventHandle);
    }

    @Override
    public boolean isActive() {
        return LinuxLibrary.get().isLoaded();
    }

    @Override
    public void uninstall() {

    }

    @Override
    public void install() {

    }
}
