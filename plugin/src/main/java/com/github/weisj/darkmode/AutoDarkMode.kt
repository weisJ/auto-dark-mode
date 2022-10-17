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
package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.*
import com.github.weisj.darkmode.platform.settings.ifPresent
import com.github.weisj.darkmode.platform.settings.letValue
import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Alarm
import javax.swing.UIManager.LookAndFeelInfo

/**
 * Automatically changes the IDEA theme based on system settings.
 */
class AutoDarkMode : Disposable, ThemeCallback {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var monitor = lazy { createMonitor() }

    private fun createMonitor(): ThemeMonitor = try {
        val serviceProvider = ApplicationManager.getApplication().getService(ThemeMonitorServiceProvider::class.java)
        val service = serviceProvider.create()
        ThemeMonitorImpl(service, this)
    } catch (e: IllegalStateException) {
        LOGGER.error(e)
        NullMonitor()
    }

    fun start() {
        monitor.letValue { it.running = true }
    }

    fun stop() {
        monitor.ifPresent { it.running = false }
    }

    fun onSettingsChange() {
        if (monitor.value is NullMonitor) {
            monitor = lazyOf(createMonitor())
        }
        monitor.letValue { it.requestUpdate() }
    }

    override fun themeChanged(isDark: Boolean, isHighContrast: Boolean) {
        val (lafTarget, colorSchemeTarget) = getTargetLaf(isDark, isHighContrast)
        resetRequests()
        scheduleRequest {
            if (GeneralThemeSettings.changeIdeTheme &&
                lafTarget != LafManager.getInstance().currentLookAndFeel
            ) {
                updateLaf(lafTarget)
            }
            if (GeneralThemeSettings.changeEditorTheme &&
                colorSchemeTarget != EditorColorsManager.getInstance().globalScheme
            ) {
                updateEditorScheme(colorSchemeTarget)
            }
        }
    }

    private fun getTargetLaf(dark: Boolean, highContrast: Boolean): Pair<LookAndFeelInfo, EditorColorsScheme> {
        return GeneralThemeSettings.run {
            when {
                highContrast && checkHighContrast -> Pair(highContrastTheme, highContrastCodeScheme)
                dark -> Pair(darkTheme, darkCodeScheme)
                else -> Pair(lightTheme, lightCodeScheme)
            }
        }
    }

    private fun updateLaf(targetLaf: LookAndFeelInfo) {
        QuickChangeLookAndFeel.switchLafAndUpdateUI(LafManager.getInstance(), targetLaf, false)
    }

    private fun updateEditorScheme(colorsScheme: EditorColorsScheme) {
        EditorColorsManager.getInstance().globalScheme = colorsScheme
    }

    private fun resetRequests() {
        alarm.cancelAllRequests()
    }

    private fun scheduleRequest(runnable: () -> Unit) {
        alarm.addRequest(runnable, Registry.intValue(INSTANT_DELAY_KEY, 0))
    }

    override fun dispose() {
        stop()
    }

    fun pluginUnloaded() {
        stop()
    }

    fun pluginLoaded() {
        start()
    }

    companion object {
        private const val INSTANT_DELAY_KEY = "ide.instant.theme.switch.delay"
        private val LOGGER = PluginLogger<AutoDarkMode>()
        private val OPTIONS = ApplicationManager.getApplication().getService(AutoDarkModeOptions::class.java)

        init {
            OPTIONS.settingsLoaded()
        }
    }
}
