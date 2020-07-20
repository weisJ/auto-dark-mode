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

import static com.github.weisj.darkmode.platform.linux.gnome.GtkVariants.guessFrom;

import com.github.weisj.darkmode.platform.ThemeMonitorService;

public class GnomeThemeMonitorService implements ThemeMonitorService {

    @Override
    public boolean isDarkThemeEnabled() {
        // TODO: Stop guessing and check the settings when available (like what is mentioned in the GtkVariants class)
        String currentTheme = GnomeNative.getCurrentTheme();
        return currentTheme.equals(guessFrom(currentTheme).get("night"));
    }

    @Override
    public boolean isHighContrastEnabled() {
        // TODO: This right now isn't exactly doable with the guessing implementation. It requires a user-accessible
        // place to
        // set which theme is their "high contrast theme"
        return false;
    }

    @Override
    public long createEventHandler(Runnable callback) {
        return GnomeNative.createEventHandler(callback);
    }

    @Override
    public void deleteEventHandler(long eventHandle) {
        GnomeNative.deleteEventHandler(eventHandle);
    }

    @Override
    public boolean isActive() {
        return GnomeLibrary.get().isLoaded();
    }

    @Override
    public void uninstall() {

    }

    @Override
    public void install() {
        GnomeNative.init();
    }
}
