/*
 * MIT License
 *
 * Copyright (c) 2020 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.weisj.darkmode.platform.macos;

import com.github.weisj.darklaf.platform.NativeUtil;
import com.github.weisj.darkmode.platform.LibraryInfo;

public class MacOSNative {

    private static final String PROJECT_NAME = "auto-dark-mode-macos";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/";
    private static final String DLL_NAME = "lib" + PROJECT_NAME + ".dylib";

    public static native boolean isDarkThemeEnabled();

    public static native boolean isHighContrastEnabled();

    public static native long createPreferenceChangeListener(final Runnable callback);

    public static native void deletePreferenceChangeListener(final long listenerPtr);

    public static native void patchAppBundle();

    public static native void unpatchAppBundle();

    public static boolean loadLibrary() {
        try {
            if (LibraryInfo.isX64) {
                NativeUtil.loadLibraryFromJar(PATH + "macos-x86-64/" + DLL_NAME);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
