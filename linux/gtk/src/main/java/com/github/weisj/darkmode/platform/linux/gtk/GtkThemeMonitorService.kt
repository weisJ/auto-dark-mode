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
package com.github.weisj.darkmode.platform.linux.gtk

import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.ThemeMonitorService
import com.github.weisj.darkmode.platform.linux.gtk.GtkVariants.guessFrom

class GtkThemeMonitorService : ThemeMonitorService {

    init {
        GtkLibrary.get()
    }

    override val isDarkThemeEnabled: Boolean
        get() {
            val currentTheme = currentGtkTheme
            return if (GtkSettings.guessLightAndDarkThemes) {
                currentTheme == guessFrom(currentTheme)[GtkVariants.Variant.Night]
            } else {
                GtkSettings.darkGtkTheme.name == currentTheme
            }
        }
    override val isHighContrastEnabled: Boolean
        get() {
            if (GtkSettings.guessLightAndDarkThemes) return false
            val currentTheme = currentGtkTheme
            return GtkSettings.highContrastGtkTheme.name == currentTheme
        }
    override val isSupported: Boolean
        get() = GtkLibrary.get().isLoaded
    val currentGtkTheme: String
        get() = GtkNative.getCurrentTheme()

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        return NativePointer(GtkNative.createEventHandler(callback))
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        GtkNative.deleteEventHandler(eventHandle.pointer)
    }

    override fun install() {
        GtkNative.init()
    }
}
