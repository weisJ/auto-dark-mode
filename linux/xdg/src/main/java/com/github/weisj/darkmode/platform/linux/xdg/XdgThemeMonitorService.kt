// based on https://gist.github.com/DevSrSouza/b013d1a8119f50615a493b36cf0b9b56

package com.github.weisj.darkmode.platform.linux.xdg

import com.github.weisj.darkmode.platform.Compatibility
import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.ThemeMonitorService
import org.freedesktop.dbus.interfaces.DBusSigHandler

class XdgThemeMonitorService : ThemeMonitorService {
    private val freedesktopConnection = FreedesktopConnection()
    private val sigHandler = SigHandler()
    override val isDarkThemeEnabled: Boolean get() = freedesktopConnection.theme == ThemeMode.DARK
    override val compatibility: Compatibility
        get() = freedesktopConnection.compatibility
    override val isHighContrastEnabled: Boolean = false  // No xdg preference for that available

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        check(sigHandler.eventHandler == null) { "Event handler already initialized" }

        freedesktopConnection.addSettingChangedHandler(sigHandler)
        sigHandler.eventHandler = callback
        return NativePointer(-1L)
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        freedesktopConnection.removeSettingChangedHandler(sigHandler)
        sigHandler.eventHandler = null
    }

    private class SigHandler : DBusSigHandler<FreedesktopInterface.SettingChanged> {
        var eventHandler: (() -> Unit)? = null
        override fun handle(signal: FreedesktopInterface.SettingChanged) {
            if (signal.colorSchemeChanged) {
                eventHandler?.invoke()
            }
        }
    }
}
