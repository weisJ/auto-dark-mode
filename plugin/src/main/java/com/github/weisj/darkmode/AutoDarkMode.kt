package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.*
import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Alarm
import javax.swing.UIManager.LookAndFeelInfo

/**
 * Automatically changes the IDEA theme based on system settings.
 */
class AutoDarkMode : Disposable, ThemeCallback {
    private val alarm = Alarm()
    private val options: AutoDarkModeOptions = ServiceManager.getService(AutoDarkModeOptions::class.java)
    private var monitorValue: ThemeMonitor? = null
    private val monitor: ThemeMonitor
        get() = monitorValue ?: createMonitor()

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
        (monitor ?: createMonitor()).isRunning = true
    }

    fun stop() {
        stop(true)
    }

    private fun stop(dispose: Boolean) {
        monitorValue?.isRunning = false;
        if (dispose) setNull()
    }

    fun onSettingsChange() {
        monitor.requestUpdate()
    }

    override fun themeChanged(isDark: Boolean, isHighContrast: Boolean) {
        val target = getTargetLaf(isDark, isHighContrast)
        if (target != LafManager.getInstance().currentLookAndFeel) {
            updateLaf(target)
        }
    }

    private fun getTargetLaf(dark: Boolean, highContrast: Boolean): LookAndFeelInfo {
        return when {
            highContrast && options.checkHighContrast -> options.highContrastTheme
            dark -> options.darkTheme
            else -> options.lightTheme
        }
    }

    private fun updateLaf(targetLaf: LookAndFeelInfo) {
        alarm.cancelAllRequests()
        alarm.addRequest(
            {
                QuickChangeLookAndFeel.switchLafAndUpdateUI(
                    LafManager.getInstance(),
                    targetLaf,
                    false
                )
            },
            Registry.get("ide.instant.theme.switch.delay").asInteger()
        )
    }

    override fun dispose() {
        stop(false)
        setNull()
    }

    private fun setNull() {
        monitorValue = null
    }

    fun uninstall() {
        stop(false)
        monitorValue?.uninstall()
        setNull()
    }

    fun install() {
        monitor.install()
        start()
    }

    companion object {
        private val LOGGER = Logger.getInstance(AutoDarkMode::class.java)
    }
}
