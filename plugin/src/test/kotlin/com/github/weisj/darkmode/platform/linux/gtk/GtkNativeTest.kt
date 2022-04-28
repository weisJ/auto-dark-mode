/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Jannis Weis
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

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.NativePointer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

const val GSETTINGS_PATH = "org.gnome.desktop.interface"
const val GSETTINGS_KEY = "gtk-theme"
const val XFCONF_QUERY_CHANNEL = "xsettings"
const val XFCONF_QUERY_PROPERTY = "/Net/ThemeName"

interface ThemeChanger {
    val currentTheme: String

    fun String.runCommand(): String {
        val process = ProcessBuilder(*split(" ").toTypedArray()).start()
        val output = process.inputStream.reader(Charsets.UTF_8).use {
            it.readText()
        }
        process.waitFor(10, TimeUnit.SECONDS)
        return output
    }

    fun String.stripQuotes(): String {
        return if (startsWith("'")) {
            trim().drop(1).dropLast(1)
        } else trim()
    }
}

class GSettingsThemeChanger : ThemeChanger {
    override var currentTheme: String
        get() = "gsettings get $GSETTINGS_PATH $GSETTINGS_KEY".runCommand().stripQuotes()
        set(value) {
            "gsettings set $GSETTINGS_PATH $GSETTINGS_KEY $value".runCommand()
        }
}

class XfConfQueryThemeChanger : ThemeChanger {
    override var currentTheme: String
        get() = "xfconf-query -c $XFCONF_QUERY_CHANNEL -p $XFCONF_QUERY_PROPERTY".runCommand().stripQuotes()
        set(value) {
            "xfconf-query -c $XFCONF_QUERY_CHANNEL -p $XFCONF_QUERY_PROPERTY -s $value".runCommand()
        }
}

class GtkNativeTest {
    // local dev: choose one and comment out the other
    private val themeChanger = GSettingsThemeChanger()
    // private val themeChanger = XfConfQueryThemeChanger()

    @Test
    @EnabledOnOs(OS.LINUX)
    fun testLibraryLoading() {
        assumeTrue(LibraryUtil.isGtk && LibraryUtil.isX64)
        assertTrue(GtkLibrary.get().isLoaded)
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun testThemeChange() {
        assumeTrue(LibraryUtil.isGtk && LibraryUtil.isX64)
        GtkLibrary.get()

        themeChanger.currentTheme = "Adwaita-dark"

        val service = GtkThemeMonitorService()
        assertTrue(service.isSupported)
        service.install()

        assertEquals(themeChanger.currentTheme, service.currentGtkTheme)
        assertTrue(service.isDarkThemeEnabled)

        val countDownLatch = CountDownLatch(1)
        val eventHandler: NativePointer = service.createEventHandler {
            countDownLatch.countDown()
        }!!

        themeChanger.currentTheme = "Adwaita"

        countDownLatch.await(10, TimeUnit.SECONDS)
        assertTrue(countDownLatch.count == 0L)

        assertEquals(themeChanger.currentTheme, service.currentGtkTheme)
        assertEquals("Adwaita", service.currentGtkTheme)
        assertFalse(service.isDarkThemeEnabled)

        service.deleteEventHandler(eventHandler)
    }
}
