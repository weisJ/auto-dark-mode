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
package com.github.weisj.darkmode.platform.macos;

import com.github.weisj.darkmode.platform.AbstractPluginLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.PluginLogger;

public class MacOSLibrary extends AbstractPluginLibrary {

    private static final String PATH = "/com/github/weisj/darkmode/auto-dark-mode-macos";
    private static final String x86_64_PATH = PATH + "/libauto-dark-mode-macos-x86-64.dylib";
    private static final String arm64_PATH = PATH + "/libauto-dark-mode-macos-arm64.dylib";

    private static final MacOSLibrary instance = new MacOSLibrary();

    static MacOSLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    protected MacOSLibrary() {
        super("auto-dark-mode-macos", PluginLogger.getLogger(MacOSLibrary.class));
    }

    @Override
    final protected Class<?> getLoaderClass() {
        return MacOSLibrary.class;
    }

    protected String getArm64Path() {
        return arm64_PATH;
    }

    protected String getX64Path() {
        return x86_64_PATH;
    }

    @Override
    public String getLibraryPath() {
        if (LibraryUtil.isX86Compatible && LibraryUtil.isX64) {
            return getX64Path();
        } else if (LibraryUtil.isM1) {
            return getArm64Path();
        } else {
            throw new IllegalStateException("Unsupported arch");
        }
    }

    @Override
    protected boolean canLoad() {
        return ((LibraryUtil.isX86Compatible && LibraryUtil.isX64) || LibraryUtil.isM1) && LibraryUtil.isMacOSMojave;
    }
}
