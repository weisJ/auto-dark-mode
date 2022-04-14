package com.github.weisj.darkmode.platform.linux.xdg

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

enum class ThemeMode {
    ERROR, DARK, LIGHT
}

@Suppress("FunctionName")
@DBusInterfaceName("org.freedesktop.portal.Settings")
interface FreedesktopInterface : DBusInterface {
    companion object {
        const val APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
        const val COLOR_SCHEME_KEY = "color-scheme"
    }

    fun Read(namespace: String, key: String): Variant<*>

    class SettingChanged(objectpath: String, namespace: String, key: String, value: Variant<Any>) :
        DBusSignal(objectpath, namespace, key, value) {
        val colorSchemeChanged: Boolean =
            namespace == APPEARANCE_NAMESPACE && key == COLOR_SCHEME_KEY
    }
}
