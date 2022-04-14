/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.weisj.darkmode.platform.linux.xdg

import com.github.weisj.darkmode.platform.Compatibility
import com.github.weisj.darkmode.platform.withContextClassLoader
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusException
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

interface FreedesktopConnection {
    val theme: ThemeMode
    val compatibility: Compatibility

    fun addSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>)
    fun removeSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>)

    companion object {
        operator fun invoke(): FreedesktopConnection {
            val connection = try {
                // Temporarily replace the current thread's contextClassLoader to work around dbus-java's naive service loading
                withContextClassLoader(Companion::class.java.classLoader) {
                    DBusConnectionBuilder.forSessionBus().build()
                }
            } catch (e: DBusException) {
                return NullFreedesktopConnection(e.message ?: "")
            } catch (e: DBusExecutionException) {
                return NullFreedesktopConnection(e.message ?: "")
            }
            return DBusFreedesktopConnection(connection)
        }
    }
}

internal class NullFreedesktopConnection(private val error: String) : FreedesktopConnection {
    override val theme = ThemeMode.ERROR
    override val compatibility: Compatibility
        get() = Compatibility(false, error)

    override fun addSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>) {
        throw UnsupportedOperationException()
    }

    override fun removeSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>) {
        throw UnsupportedOperationException()
    }

}

internal class DBusFreedesktopConnection(
    private val connection: DBusConnection
) : FreedesktopConnection {

    private val freedesktopInterface: FreedesktopInterface? = connection.getRemoteObject(
        "org.freedesktop.portal.Desktop",
        "/org/freedesktop/portal/desktop",
        FreedesktopInterface::class.java
    )

    override val theme: ThemeMode
        get() {
            freedesktopInterface ?: return ThemeMode.ERROR

            val theme = freedesktopInterface.runCatching {
                recursiveVariantValue(
                    Read(
                        FreedesktopInterface.APPEARANCE_NAMESPACE,
                        FreedesktopInterface.COLOR_SCHEME_KEY
                    )
                ) as UInt32
            }.getOrElse { return ThemeMode.ERROR }

            return when (theme.toInt()) {
                1 -> ThemeMode.DARK
                else -> ThemeMode.LIGHT
            }
        }

    override val compatibility: Compatibility
        get() {
            freedesktopInterface ?: return Compatibility(false, "FreedesktopInterface could not be created")
            if (theme == ThemeMode.ERROR) return Compatibility(false, "Internal error while retrieving theme")
            return Compatibility(true, "")
        }

    override fun addSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>) {
        connection.addSigHandler(FreedesktopInterface.SettingChanged::class.java, sigHandler)
    }

    override fun removeSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>) =
        connection.removeSigHandler(FreedesktopInterface.SettingChanged::class.java, sigHandler)

    /**
     * Unpacks a Variant recursively and returns the inner value.
     * @see Variant
     */
    private fun recursiveVariantValue(variant: Variant<*>): Any {
        val value = variant.value
        return if (value !is Variant<*>) value else recursiveVariantValue(value)
    }
}
