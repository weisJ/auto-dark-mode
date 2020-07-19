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

/**
 * This class is a no-op {@link ThemeMonitorService}. This class
 * is used when a ThemeMonitorService needs to be created for a given environment but no
 * ThemeMonitorService has been created to suit that environment yet.
 * <p>
 * By returning {@code false} from {@link #isActive()}, this class signals to {@link AbstractThemeMonitor} that
 * delegation failed and no suitable
 * ThemeMonitorService could be found for the current environment.
 */
public class NullThemeMonitorService implements ThemeMonitorService {
    @Override
    public boolean isDarkThemeEnabled() {
        return false;
    }

    @Override
    public boolean isHighContrastEnabled() {
        return false;
    }

    @Override
    public long createEventHandler(Runnable callback) {
        return 0;
    }

    @Override
    public void deleteEventHandler(long eventHandle) {}

    @Override
    public boolean isActive() {
        return false;
    }
}
