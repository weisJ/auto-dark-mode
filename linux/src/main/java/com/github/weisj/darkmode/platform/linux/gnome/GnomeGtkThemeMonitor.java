package com.github.weisj.darkmode.platform.linux.gnome;

import com.github.weisj.darkmode.platform.ThemeMonitorService;
import com.github.weisj.darkmode.platform.linux.util.ProcessResult;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.weisj.darkmode.platform.LibraryUtil.userHomeDirectory;
import static com.github.weisj.darkmode.platform.linux.util.CmdlineUtils.exec;
import static java.util.Objects.isNull;

/**
 * This class monitors the GTK theme which is set for the current Gnome environment and makes an estimated guess as to
 * whether the GTK theme is meant to resemble a "light theme", "dark theme", and whether the theme is "high contrast".
 * <p>
 * TODO: Remove the guessing feature in favor of a settings panel where users can specify their themes themselves.
 */
public class GnomeGtkThemeMonitor implements Runnable {
    private final static Logger logger = Logger.getLogger(GnomeGtkThemeMonitor.class.getName());
    private final WatchService watchService;
    private final Path path = Paths.get(userHomeDirectory + "/.config/dconf");
    private final String dconfSubPath = "/org/gnome/desktop/interface/";
    private final String schemaName = "org.gnome.desktop.interface";
    private final String gtkThemeKey = "gtk-theme";
    private final String getGtkThemeCommand;
    private volatile boolean stopping;
    private Runnable callback;

    /**
     * Creates a {@link Runnable} which monitors binary file found at "~/.config/dconf/user" which holds the
     * user-related settings (there are other data locations too which can be found via the $XDG_DATA_DIRS environment
     * variable) that are accessed when interfacing with GSettings (or Gio.Settings).
     * <p>
     * The {@code user} binary will never be directly updated. Instead, it will be recreated with updated values. The
     * order of events is as follows.
     * <p>
     * 1. An {@code ENTRY_CREATE} event is fired off for a file called {@code user.FR57M0} where {@code FR57M0} is any
     * length six combination of letters and numbers. This part of the name is different each time.
     * <p>
     * 2. An {@code ENTRY_MODIFY} event occurs (this can sometimes be fired twice for reasons unknown) for {@code
     * user.FR57M0}
     * <p>
     * 3. An {@code ENTRY_DELETE} event occurs for {@code user.FR57M0}
     * <p>
     * 4. An {@code ENTRY_CREATE} event occurs for the {@code user} file
     *
     * @throws IOException if an IOException occurs while creating a {@link WatchService} or registering the path with
     *                     the WatchService
     */
    public GnomeGtkThemeMonitor() throws IOException, InterruptedException {
        boolean gsettingsCommandPresent = exec("which gsettings").getExitCode() == 0;
        boolean dconfCommandPresent = exec("which dconf").getExitCode() == 0;
        if (!gsettingsCommandPresent && !dconfCommandPresent) {
            throw new IllegalStateException(
                "To use Auto Dark Mode with Gnome, the 'gsettings' command or the 'dconf' command "
                + "must be available. However, neither of them were found.");
        }
        if (gsettingsCommandPresent) {
            getGtkThemeCommand = "gsettings get " + schemaName + " " + gtkThemeKey;
        } else {
            getGtkThemeCommand = "dconf read " + dconfSubPath + gtkThemeKey;
        }
        watchService = FileSystems.getDefault().newWatchService();
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE);
    }

    /**
     * @param callback the callback specified by {@link ThemeMonitorService#createEventHandler(Runnable)} which should
     *                 be called in the event that the GTK theme is changed.
     */
    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public void deleteEventHandler(long eventHandle) {
        stopping = true;
        this.callback = null;
        try {
            watchService.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred while deleting the event handler."
                                     + "May not have closed completely.");
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Reads the current GTK theme via a command to either dconf or gsettings.
     *
     * @return the name of the current GTK theme, "Pop" for instance.
     */
    String readGtkTheme() {
        ProcessResult commandResult;
        try {
            commandResult = exec(getGtkThemeCommand);
            if (commandResult.getExitCode() != 0) {
                logger.log(Level.SEVERE,
                           "The following command exited with exit code " + commandResult.getExitCode() + ": "
                           + getGtkThemeCommand);
                logger.log(Level.SEVERE, "Contents of stdOut:\n" + commandResult.getStdOut());
                logger.log(Level.SEVERE, "Contents of stdErr:\n" + commandResult.getStdErr());
                return "";
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return "";
        }
        /*
         * The result of the command surrounds the theme name with apostrophes. This removes it and removes
         * the trailing newline that also accompanies the execution result.
         */
        return commandResult.getStdOut().replace("'", "").trim();
    }

    /**
     * This function reads the current GTK theme with {@link #readGtkTheme()} and calls the provided callback only if
     * the provided String {@code lastKnownThemeName} differs from the result of calling {@link #readGtkTheme()}. This
     * extra logic is necessary because this method will be called on <i>any</i> modification of settings binary
     * monitored by this class. There is no efficient way to know whether or not the "gtk-theme" key has changed in the
     * settings file without retrieving it and checking it ourselves. Parsing the file itself to determine this would be
     * very resource intensive.
     *
     * @param lastKnownThemeName the last known theme name. By keeping track of the last known theme, we know whether or
     *                           not it's actually necessary to run the callback. This string is allowed to be null
     *                           which will certainly not be equal to the result of {@link #readGtkTheme()}. The string
     *                           would be null in the case that the plugin has just started and there is no "last known"
     *                           theme yet.
     * @param callback           a callback to run if the theme has changed
     * @return the result of the most recent {@link #readGtkTheme()} call
     */
    private String callOnNotificationCallbackIfNecessary(String lastKnownThemeName, Runnable callback) {
        String currentGtkTheme = readGtkTheme();
        if (!currentGtkTheme.equals(lastKnownThemeName)) {
            callback.run();
        }
        return currentGtkTheme;
    }

    @Override
    public void run() {
        String lastKnownThemeName = null;
        if (isNull(this.callback)) {
            logger.log(Level.SEVERE, "Tried to start GnomeGtkThemeMonitor without the onNotification callback");
            return;
        }
        try {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context() instanceof Path) {
                        Path eventPath = (Path) event.context();
                        if (eventPath.getFileName().toString().equals("user")) {
                            lastKnownThemeName = callOnNotificationCallbackIfNecessary(lastKnownThemeName, callback);
                        }
                    }
                }
                key.reset();
            }
        } catch (ClosedWatchServiceException | InterruptedException e) {
            //If stopping is true, this exception is expected and is caused by us interrupting the WatchService
            if (!stopping) {
                //Something's wrong
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}
