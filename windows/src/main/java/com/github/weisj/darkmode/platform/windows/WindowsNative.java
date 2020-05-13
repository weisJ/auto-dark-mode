package com.github.weisj.darkmode.platform.windows;

import com.github.weisj.darklaf.platform.NativeUtil;
import com.github.weisj.darkmode.platform.LibraryInfo;

import java.io.IOException;

public final class WindowsNative {

    private static final String PROJECT_NAME = "auto-dark-mode-windows";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/";
    private static final String DLL_NAME = PROJECT_NAME + ".dll";

    static native boolean isDarkThemeEnabled();

    static native boolean isHighContrastEnabled();

    static native long createEventHandler(final Runnable callback);

    static native void deleteEventHandler(final long handle);

    static boolean loadLibrary() {
        try {
            if (LibraryInfo.isX86) {
                NativeUtil.loadLibraryFromJar(PATH + "windows-x86/" + DLL_NAME);
            } else if (LibraryInfo.isX64) {
                NativeUtil.loadLibraryFromJar(PATH + "windows-x86-64/" + DLL_NAME);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
