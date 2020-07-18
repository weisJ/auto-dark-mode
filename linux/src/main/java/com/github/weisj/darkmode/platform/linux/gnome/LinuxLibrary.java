package com.github.weisj.darkmode.platform.linux.gnome;

import com.github.weisj.darklaf.platform.AbstractLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;

import java.util.logging.Logger;

/*
 * TODO: This class makes the same assumption as the build script, that Gnome is
 *  the only desktop environment. The auto-dark-mode-linux module may need to be
 *  split into subprojects to accommodate for different .so libraries used
 *  for different environments.
 */
public class LinuxLibrary extends AbstractLibrary {

    private static final String PROJECT_NAME = "auto-dark-mode-linux";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/linux-x86-64/";
    private static final String DLL_NAME = "lib" + PROJECT_NAME + ".so";
    private static final LinuxLibrary instance = new LinuxLibrary();

    protected LinuxLibrary() {
        super(PATH, DLL_NAME, Logger.getLogger(LinuxLibrary.class.getName()));
    }

    static LinuxLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    @Override
    protected boolean canLoad() {
        return LibraryUtil.isX64 && LibraryUtil.isGnome;
    }
}
