package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.*
import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Alarm
import javax.swing.UIManager.LookAndFeelInfo

/**
 * Automatically changes the IDEA theme based on system settings.
 */
class AutoDarkMode : Disposable, ThemeCallback {
    private val alarm = Alarm()
    private val monitor = lazy { createMonitor() }

    private fun createMonitor(): ThemeMonitor {
        return try {
            val service = ServiceManager.getService(ThemeMonitorService::class.java)
            AbstractThemeMonitor(service, this)
        } catch (e: IllegalStateException) {
            LOGGER.error(e)
            NullMonitor()
        }
    }

    fun start() {
        monitor.value.isRunning = true
    }

    fun stop() {
        if (monitor.isInitialized()) monitor.value.isRunning = false
    }

    fun onSettingsChange() {
        monitor.value.requestUpdate()
    }

    override fun themeChanged(isDark: Boolean, isHighContrast: Boolean) {
        val (lafTarget, colorSchemeTarget) = getTargetLaf(isDark, isHighContrast)
        resetRequests()
        if (GeneralThemeSettings.changeIdeTheme
            && lafTarget != LafManager.getInstance().currentLookAndFeel
        ) {
            updateLaf(lafTarget)
        }
        if (GeneralThemeSettings.changeEditorTheme
            && colorSchemeTarget != EditorColorsManager.getInstance().globalScheme
        ) {
            updateEditorScheme(colorSchemeTarget)
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
        scheduleRequest {
            QuickChangeLookAndFeel.switchLafAndUpdateUI(LafManager.getInstance(), targetLaf, false)
        }
    }

    private fun updateEditorScheme(colorsScheme: EditorColorsScheme) {
        scheduleRequest {
            EditorColorsManager.getInstance().globalScheme = colorsScheme
        }
    }

    private fun resetRequests() {
        alarm.cancelAllRequests()
    }

    private fun scheduleRequest(runnable: () -> Unit) {
        alarm.addRequest(runnable, Registry.intValue("ide.instant.theme.switch.delay", 0))
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
        private val LOGGER = PluginLogger.getLogger(AutoDarkMode::class.java)
        private val OPTIONS = ServiceManager.getService(AutoDarkModeOptions::class.java)

        init {
            OPTIONS.settingsLoaded()
        }
    }
}
