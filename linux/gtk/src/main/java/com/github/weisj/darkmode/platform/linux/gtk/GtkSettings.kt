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
package com.github.weisj.darkmode.platform.linux.gtk

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.Notifications
import com.github.weisj.darkmode.platform.OneTimeAction
import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService

@AutoService(SettingsContainerProvider::class)
class GtkSettingsProvider : SingletonSettingsContainerProvider({ GtkSettings }, enabled = LibraryUtil.isGtk)

data class GtkTheme(val name: String) : Comparable<GtkTheme> {
    override fun compareTo(other: GtkTheme): Int = name.compareTo(other.name)
}

// `identifier` kept at "gnome_settings" for backwards compatibility with existing settings
object GtkSettings : DefaultSettingsContainer(identifier = "gnome_settings") {

    /**
     * This enum holds default values for the light, dark, and high contrast GTK themes.
     * The defaults are a safe bet as they are included with almost any install of GTK.
     * Even if they're not present, the way the logic works in GtkThemeMonitorService,
     * there won't be an issue and the plugin will fall back on the light theme.
     *
     * **See:** [Arch Wiki on GTK](https://wiki.archlinux.org/index.php/GTK#Themes)
     */
    private enum class DefaultGtkTheme(val info: GtkTheme) {
        DARK(GtkTheme("Adwaita-dark")),
        LIGHT(GtkTheme("Adwaita")),
        HIGH_CONTRAST(GtkTheme("HighContrast")),
    }

    private const val DEFAULT_GUESS_LIGHT_AND_DARK_THEMES = true

    @JvmField
    var darkGtkTheme = DefaultGtkTheme.DARK.info

    @JvmField
    var lightGtkTheme = DefaultGtkTheme.LIGHT.info

    @JvmField
    var highContrastGtkTheme = DefaultGtkTheme.HIGH_CONTRAST.info

    @JvmField
    var guessLightAndDarkThemes = DEFAULT_GUESS_LIGHT_AND_DARK_THEMES

    /*
     * Notify user about guessing mechanism. This notice should only be logged once.
     */
    private val guessingMechanismLogAction = OneTimeAction {
        Notifications.dispatchNotification(
            """
            Auto Dark Mode is currently guessing whether the current Gtk theme
            is dark or light. You can explicitly specify your light, dark and high-contrast
            theme in <nobr>"File | Settings | Auto Dark Mode"</nobr> for better results.
            """.trimIndent(),
            showSettingsLink = true
        )
    }

    init {
        if (!GtkLibrary.get().isLoaded) {
            throw IllegalStateException("Gtk library not loaded.")
        }
        group("Gtk Theme") {
            val installedGtkThemesProvider = { loadInstalledGtkThemes() }

            val gtkThemeRenderer = GtkTheme::name
            val gtkThemeTransformer = transformerOf(write = ::parseGtkTheme, read = ::readGtkTheme.or(""))

            persistentBooleanProperty(
                description = "Guess light/dark theme based on name",
                value = ::guessLightAndDarkThemes
            )

            group {
                activeIf(::guessLightAndDarkThemes.isFalse())

                persistentChoiceProperty(
                    description = "Light GTK Theme",
                    value = ::lightGtkTheme,
                    transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.LIGHT.info)
                ) { choicesProvider = installedGtkThemesProvider; renderer = gtkThemeRenderer }
                persistentChoiceProperty(
                    description = "Dark GTK Theme",
                    value = ::darkGtkTheme,
                    transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.DARK.info)
                ) { choicesProvider = installedGtkThemesProvider; renderer = gtkThemeRenderer }
                persistentChoiceProperty(
                    description = "High Contrast GTK Theme",
                    value = ::highContrastGtkTheme,
                    transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.HIGH_CONTRAST.info)
                ) { choicesProvider = installedGtkThemesProvider; renderer = gtkThemeRenderer }
            }
        }

        hidden {
            persistentBooleanProperty(value = guessingMechanismLogAction::executed)
        }
    }

    private fun loadInstalledGtkThemes(): List<GtkTheme> {
        val installedThemes = GtkThemeUtils.getInstalledThemes()
        /*
         * The default themes are added to this list. They would already be added to the list because of their
         * presence when initializing the `themes` vector in GtkThemeUtils.cpp but because they are not
         * the same instance as the defaults, the dropdown list would default to random themes because
         * the instances of the three defaults couldn't be found in ChoiceProperty#choices.
         * For this reason, the default themes that the native code adds to this list are overwritten
         * with the instances created inside the enum constructors of DefaultGtkTheme.
         */
        return mutableSetOf(DefaultGtkTheme.DARK.info, DefaultGtkTheme.LIGHT.info, DefaultGtkTheme.HIGH_CONTRAST.info)
            .apply { addAll(installedThemes.map { GtkTheme(it) }) }
            .toList()
            .sorted()
    }

    override fun onSettingsLoaded() {
        guessingMechanismLogAction()
    }

    private fun readGtkTheme(info: GtkTheme): String = info.name

    private fun parseGtkTheme(name: String): GtkTheme? = GtkTheme(name)
}
