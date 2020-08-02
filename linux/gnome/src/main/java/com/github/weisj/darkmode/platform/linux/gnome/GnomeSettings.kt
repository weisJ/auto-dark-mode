package com.github.weisj.darkmode.platform.linux.gnome

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService
import kotlin.streams.toList

/**
 * Workaround for the fact that auto service currently doesn't work with singleton objects.
 * https://github.com/google/auto/issues/785
 */
@AutoService(SettingsContainerProvider::class)
class GnomeSettingsProvider : SingletonSettingsContainerProvider({ GnomeSettings })

fun <T> concatenate(vararg lists: List<T>): List<T> {
    val result: MutableList<T> = ArrayList()
    lists.forEach { list: List<T> -> result.addAll(list) }
    return result
}

object GnomeSettings : DefaultSettingsContainer() {

    /**
     * This enum holds default values for the light, dark, and high contrast GTK themes.
     * The defaults are a safe bet as they are included with almost any install of GTK.
     * Even if they're not present, the way the logic works in GnomeThemeMonitorService,
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

    init {
        if (!GnomeLibrary.get().isLoaded) {
            throw IllegalStateException("Gnome library not loaded.")
        }
        group("Gnome Theme") {
            val installedThemes = GnomeThemeUtils.getInstalledThemes()
            /*
             * The default themes are added to this list. They would already be added to the list because of their
             * presence when initializing the `themes` vector in GnomeThemeUtils.cpp but because they are not
             * the same instance as the defaults, the dropdown list would default to random themes because
             * the instances of the three defaults couldn't be found in ChoiceProperty#choices.
             */
            val installedGtkThemes = concatenate(
                listOf(DefaultGtkTheme.DARK.info, DefaultGtkTheme.LIGHT.info, DefaultGtkTheme.HIGH_CONTRAST.info),
                installedThemes.stream().map { t -> GtkTheme(t) }.toList()
            ).distinctBy { it.name }
            val gtkThemeRenderer = GtkTheme::getName
            val gtkThemeTransformer = transformerOf(write = ::parseGtkTheme, read = ::readGtkTheme.or(""))

            persistentChoiceProperty(
                description = "Light GTK Theme",
                value = ::lightGtkTheme,
                transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.LIGHT.info)
            ) { choices = installedGtkThemes; renderer = gtkThemeRenderer }
            persistentChoiceProperty(
                description = "Dark GTK Theme",
                value = ::darkGtkTheme,
                transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.DARK.info)
            ) { choices = installedGtkThemes; renderer = gtkThemeRenderer }
            persistentChoiceProperty(
                description = "High Contrast GTK Theme",
                value = ::highContrastGtkTheme,
                transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.HIGH_CONTRAST.info)
            ) { choices = installedGtkThemes; renderer = gtkThemeRenderer }

            persistentBooleanProperty(
                description = "Guess light/dark theme based on name",
                value = ::guessLightAndDarkThemes
            ) {
                control(withProperty(::lightGtkTheme), withProperty(::darkGtkTheme), withProperty(::highContrastGtkTheme))
            }
        }
    }

    private fun readGtkTheme(info: GtkTheme): String = info.name

    private fun parseGtkTheme(name: String?): GtkTheme? = GtkTheme(name)
}
