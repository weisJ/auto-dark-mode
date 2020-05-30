package com.github.weisj.darkmode.platform.windows;

import com.github.weisj.darklaf.platform.AbstractLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;

import java.util.logging.Logger;

public class WindowsLibrary extends AbstractLibrary {

    private static final String PROJECT_NAME = "auto-dark-mode-windows";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/";
    private static final String DLL_NAME = PROJECT_NAME + ".dll";
    private static final String x86_PATH = "windows-x86/";
    private static final String x86_64_PATH = "windows-x86-64/";
    private static final WindowsLibrary instance = new WindowsLibrary();

    static WindowsLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    protected WindowsLibrary() {
        super(PATH, DLL_NAME, Logger.getLogger(WindowsLibrary.class.getName()));
    }

    @Override
    protected String getPath() {
        if (LibraryUtil.isX86) {
            return super.getPath() + x86_PATH;
        } else if (LibraryUtil.isX64) {
            return super.getPath() + x86_64_PATH;
        } else {
            return super.getPath();
        }
    }

    @Override
    protected boolean canLoad() {
        return LibraryUtil.isWin10OrNewer && !LibraryUtil.undefinedArchitecture;
    }
}
