/*
 * MIT License
 *
 * Copyright (c) 2020-2023 Jannis Weis
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
package com.github.weisj.darkmode.platform.linux.gtk;

import com.github.weisj.darkmode.platform.AbstractPluginLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.PluginLogger;

public class GtkLibrary extends AbstractPluginLibrary {

    private static final String PATH = "/com/github/weisj/darkmode/auto-dark-mode-linux-gtk";
    private static final String x86_64_PATH = PATH + "/libauto-dark-mode-linux-gtk-x86-64.so";
    private static final GtkLibrary instance = new GtkLibrary();
    private boolean laxLoadingModeEnabled = false;

    protected GtkLibrary() {
        super("auto-dark-mode-linux-gtk", PluginLogger.getLogger(GtkLibrary.class));
    }

    static void setLaxLoadingMode(boolean useLaxLoadingMode) {
        instance.laxLoadingModeEnabled = useLaxLoadingMode;
    }

    static GtkLibrary get(boolean useLaxLoadingMode) {
        if (useLaxLoadingMode && !instance.isLoaded()) {
            setLaxLoadingMode(true);
        }
        instance.updateLibrary();
        return instance;
    }

    @Override
    protected Class<?> getLoaderClass() {
        return GtkLibrary.class;
    }

    @Override
    public String getLibraryPath() {
        return x86_64_PATH;
    }

    @Override
    protected boolean canLoad() {
        return LibraryUtil.isX64 && (LibraryUtil.isGtk || laxLoadingModeEnabled);
    }
}
