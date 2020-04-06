package com.github.weisj.darkmode;

import com.github.weisj.darklaf.platform.NativeUtil;

import java.io.IOException;

public final class DarkModeNative {

    private static final String PROJECT_NAME = "auto-dark-mode-plugin";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/";
    private static final String DLL_NAME = PROJECT_NAME + ".dll";

    private static final String jreArchitecture = System.getProperty("sun.arch.data.model");
    private static final boolean isX86;
    private static final boolean isX64;
    private static final String X86 = "32";
    private static final String X64 = "64";

    static {
        isX64 = X64.equals(jreArchitecture);
        isX86 = X86.equals(jreArchitecture);
    }

    public static native boolean isDarkThemeEnabled();

    public static native boolean isHighContrastEnabled();

    public static native long createEventHandler(final Runnable callback);

    public static native void deleteEventHandler(final long handle);


    public static boolean loadLibrary() {
        try {
            if (isX86) {
                NativeUtil.loadLibraryFromJar(PATH + "windows-x86/" + DLL_NAME);
            } else if (isX64) {
                NativeUtil.loadLibraryFromJar(PATH + "windows-x86-64/" + DLL_NAME);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
