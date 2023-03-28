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
package com.github.weisj.darkmode.platform.macos

import com.github.weisj.darkmode.platform.Compatibility
import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.ThemeMonitorService
import com.github.weisj.darkmode.platform.ThemeMonitorServiceProvider

class MacOSThemeMonitorServiceProvider : ThemeMonitorServiceProvider {
    override fun create(): ThemeMonitorService = MacOSThemeMonitorService()
}


class MacOSThemeMonitorService : ThemeMonitorService {
    override val isDarkThemeEnabled: Boolean
        get() = MacOSNative.isDarkThemeEnabled()
    override val isHighContrastEnabled: Boolean
        get() = MacOSNative.isHighContrastEnabled()
    override val compatibility: Compatibility = if (MacOSLibrary.get().isLoaded) {
        Compatibility(true, "")
    } else {
        Compatibility(false, "MacOS Library couldn't be loaded")
    }

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        return NativePointer(MacOSNative.createPreferenceChangeListener(callback))
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        MacOSNative.deletePreferenceChangeListener(eventHandle.pointer)
    }

    override fun install() {
        if (LibraryUtil.isMacOSCatalina) {
            MacOSNative.patchAppBundle()
        }
    }
}
