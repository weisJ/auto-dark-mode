/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.weisj.darkmode.platform.windows;

import com.github.weisj.darkmode.platform.AbstractPluginLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.PluginLogger;

@SuppressWarnings({"java:S115", "java:S1075"})
public class WindowsLibrary extends AbstractPluginLibrary {

    private static final String PATH = "/com/github/weisj/darkmode/auto-dark-mode-windows";
    private static final String x86_PATH = PATH + "/auto-dark-mode-windows-x86.dll";
    private static final String x86_64_PATH = PATH + "/auto-dark-mode-windows-x86-64.dll";
    private static final WindowsLibrary instance = new WindowsLibrary();

    static WindowsLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    protected WindowsLibrary() {
        super("auto-dark-mode-windows", PluginLogger.getLogger(WindowsLibrary.class));
    }

    @Override
    final protected Class<?> getLoaderClass() {
        return WindowsLibrary.class;
    }

    protected String getX86Path() {
        return x86_PATH;
    }

    protected String getX64Path() {
        return x86_64_PATH;
    }

    @Override
    public String getLibraryPath() {
        if (LibraryUtil.isX86) {
            return getX86Path();
        } else if (LibraryUtil.isX64) {
            return getX64Path();
        } else {
            throw new IllegalStateException("Unsupported arch");
        }
    }

    @Override
    protected boolean canLoad() {
        return LibraryUtil.isWin10OrNewer && LibraryUtil.isX86Compatible;
    }
}
