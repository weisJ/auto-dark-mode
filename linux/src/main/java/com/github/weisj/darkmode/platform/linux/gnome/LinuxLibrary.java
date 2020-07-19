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
package com.github.weisj.darkmode.platform.linux.gnome;

import java.util.logging.Logger;

import com.github.weisj.darklaf.platform.AbstractLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;

/*
 * TODO: This class makes the same assumption as the build script, that Gnome is
 * the only desktop environment. The auto-dark-mode-linux module may need to be
 * split into subprojects to accommodate for different .so libraries used
 * for different environments.
 */
public class LinuxLibrary extends AbstractLibrary {

    private static final String PROJECT_NAME = "auto-dark-mode-linux";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/linux-x86-64/";
    private static final String DLL_NAME = "lib" + PROJECT_NAME + ".so";
    private static final LinuxLibrary instance = new LinuxLibrary();

    protected LinuxLibrary() {
        super(PATH, DLL_NAME, Logger.getLogger(LinuxLibrary.class.getName()));
    }

    static LinuxLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    @Override
    protected boolean canLoad() {
        return LibraryUtil.isX64 && LibraryUtil.isGnome;
    }
}
