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
package com.github.weisj.darkmode.platform.linux

import com.github.weisj.darkmode.platform.Notifications
import com.github.weisj.darkmode.platform.NullThemeMonitorService
import com.github.weisj.darkmode.platform.ThemeMonitorService
import com.github.weisj.darkmode.platform.ThemeMonitorServiceProvider
import com.github.weisj.darkmode.platform.linux.gtk.GtkThemeMonitorService
import com.github.weisj.darkmode.platform.linux.gtk.SignalType
import com.github.weisj.darkmode.platform.linux.xdg.XdgThemeMonitorService

class LinuxThemeMonitorServiceProvider : ThemeMonitorServiceProvider {
    override fun create(): ThemeMonitorService = createCompatibleMonitorService()

    private fun createCompatibleMonitorService(): ThemeMonitorService {
        when (AdvancedLinuxSettings.implType) {
            ImplementationType.GTK_XSETTINGS ->
                return GtkThemeMonitorService(SignalType.GTK)

            ImplementationType.GTK_GSETTINGS ->
                return GtkThemeMonitorService(SignalType.GIO)

            ImplementationType.XDG_DESKTOP -> {
                val xdgThemeMonitorService = XdgThemeMonitorService()
                if (xdgThemeMonitorService.compatibility.isSupported) {
                    return xdgThemeMonitorService
                }
            }
        }

        Notifications.dispatchNotification(
            message = "No appropriate implementation could be selected. Please check the settings",
            showSettingsLink = true
        )
        return NullThemeMonitorService()
    }

    override fun isStillValid(impl: ThemeMonitorService?): Boolean {
        val implType = AdvancedLinuxSettings.implType
        return when {
            impl is GtkThemeMonitorService && impl.signalType == SignalType.GTK ->
                implType == ImplementationType.GTK_XSETTINGS

            impl is GtkThemeMonitorService && impl.signalType == SignalType.GIO ->
                implType == ImplementationType.GTK_GSETTINGS

            impl is XdgThemeMonitorService -> implType == ImplementationType.XDG_DESKTOP
            else -> false
        }
    }
}

