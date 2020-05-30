package com.github.weisj.darkmode.platform.macos;

import com.github.weisj.darklaf.platform.AbstractLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;

import java.util.logging.Logger;

public class MacOSLibrary extends AbstractLibrary {

    private static final String PROJECT_NAME = "auto-dark-mode-macos";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/";
    private static final String DLL_NAME = "lib" + PROJECT_NAME + ".dylib";
    private static final MacOSLibrary instance = new MacOSLibrary();

    static MacOSLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    protected MacOSLibrary() {
        super(PATH, DLL_NAME, Logger.getLogger(MacOSLibrary.class.getName()));
    }

    @Override
    protected boolean canLoad() {
        return LibraryUtil.isX64 && LibraryUtil.isMacOSMojave;
    }
}
