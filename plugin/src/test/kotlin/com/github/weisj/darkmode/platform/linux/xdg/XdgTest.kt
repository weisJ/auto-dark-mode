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

import com.github.weisj.darkmode.platform.linux.GSettingsThemeChanger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

const val LIGHT_THEME = "org.kde.breeze.desktop"
const val DARK_THEME = "org.kde.breezedark.desktop"

@EnabledOnOs(OS.LINUX)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XdgTest {
    // local dev: choose one and comment out the other
    // also change the light and dark themes accordingly
    private val themeChanger = GSettingsThemeChanger()
    // private val themeChanger = XfConfQueryThemeChanger()
    //private val themeChanger = KdeThemeChanger()

    private val instance = XdgThemeMonitorService()
    // used to switch back to the user set theme for convenience. Assumes that the tests pass
    private var userTheme = ""

    @BeforeAll
    fun setUp() {
        // save current user theme
        userTheme = themeChanger.currentTheme
    }

    @AfterAll
    internal fun tearDownAll() {
        // return to user theme
        if (userTheme == "") return
        themeChanger.currentTheme = userTheme
    }

    @Test
    fun testAvailable() {
        assertTrue(instance.compatibility.isSupported)
    }

    @Test
    fun testThemeDetected() {
        assumeTrue(instance.compatibility.isSupported)

        themeChanger.currentTheme = DARK_THEME
        assertTrue(instance.isDarkThemeEnabled)

        themeChanger.currentTheme = LIGHT_THEME
        assertFalse(instance.isDarkThemeEnabled)
    }

    @Test
    fun testThemeChange() {
        assumeTrue(instance.compatibility.isSupported)

        themeChanger.currentTheme = LIGHT_THEME

        var themeChanged = false
        val pointer = instance.createEventHandler {
            themeChanged = true
        }
        themeChanger.currentTheme = DARK_THEME

        assertTrue(themeChanged)
        instance.deleteEventHandler(pointer!!)
    }
}
