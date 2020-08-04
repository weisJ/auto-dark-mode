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
 *
 */
package com.github.weisj.darkmode.platform;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.SystemProperties;

public final class LibraryUtil {

    public static final String jreArchitecture = System.getProperty("sun.arch.data.model");
    public static final String userHomeDirectory = SystemProperties.getUserHome();
    public static final boolean isX86;
    public static final boolean isX64;
    public static final boolean undefinedArchitecture;
    public static final String X86 = "32";
    public static final String X64 = "64";
    public static boolean isWin10OrNewer = SystemInfo.isWin10OrNewer;
    public static boolean isMacOSMojave = SystemInfo.isMacOSMojave;
    public static boolean isMacOSCatalina = SystemInfo.isMacOSCatalina;
    public static boolean isGnome = SystemInfo.isGNOME;

    static {
        isX64 = X64.equals(jreArchitecture);
        isX86 = X86.equals(jreArchitecture);
        undefinedArchitecture = !isX64 && !isX86;
    }
}
