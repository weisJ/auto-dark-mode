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
package com.github.weisj.darkmode.platform.windows;

import com.github.weisj.darkmode.platform.AbstractPluginLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.PluginLogger;

public class WindowsLibrary extends AbstractPluginLibrary {

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
        super(PATH, DLL_NAME, PluginLogger.getLogger(WindowsLibrary.class));
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
