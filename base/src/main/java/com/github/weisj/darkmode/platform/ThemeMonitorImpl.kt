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
package com.github.weisj.darkmode.platform

class ThemeMonitorImpl(
    private val monitorService: ThemeMonitorService,
    private val onThemeChange: ThemeCallback
) : ThemeMonitor {

    private var state: MonitorState = MonitorState()
    private var listenerHandle: NativePointer? = null
    override var running: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                start()
            } else {
                stop()
            }
        }

    init {
        check(monitorService.isSupported) { "Monitoring is not supported." }
        monitorService.install()
    }

    private fun onNotification(forceChange: Boolean = false) {
        val newState = monitorService.getState()
        val hasChanged = state != newState
        LOGGER.info("Received notification")
        if (hasChanged || forceChange) {
            state = newState
            onThemeChange.themeChanged(state.dark, state.highContrast)
        }
    }

    override fun requestUpdate() {
        onNotification(true)
    }

    private fun start() {
        state = monitorService.getState()
        listenerHandle = monitorService.createEventHandler { onNotification() }
        if (listenerHandle == null) {
            LOGGER.error("Could not create notification listener. Monitoring will not be started")
            return
        }
        onThemeChange.themeChanged(state.dark, state.highContrast)
        LOGGER.info("Started theme monitoring.")
    }

    private fun stop() {
        LOGGER.info("Stopped theme monitoring.")
        listenerHandle?.let { monitorService.deleteEventHandler(it) }
    }

    companion object {
        private val LOGGER = PluginLogger<ThemeMonitorImpl>()
    }

    private fun ThemeMonitorService.getState(): MonitorState = MonitorState(isDarkThemeEnabled, isHighContrastEnabled)

    data class MonitorState(val dark: Boolean = false, val highContrast: Boolean = false)
}
