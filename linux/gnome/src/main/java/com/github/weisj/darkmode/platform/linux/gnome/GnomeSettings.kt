package com.github.weisj.darkmode.platform.linux.gnome

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService
import kotlin.streams.toList

/**
 * Workaround for the fact that auto service currently doesn't work with singleton objects.
 * https://github.com/google/auto/issues/785
 */
@AutoService(SettingsContainer::class)
class GnomeSettingsProxy : SettingsContainer by GnomeSettings

object GnomeSettings : DefaultSettingsContainer() {
    override val enabled: Boolean
        get() = super.enabled && LibraryUtil.isGnome

    /**
     * This enum holds default values for the light, dark, and high contrast GTK themes.
     * The defaults are a safe bet as they are included with almost any install of GTK.
     * Even if they're not present, the way the logic works in GnomeThemeMonitorService,
     * there won't be an issue and the plugin will fall back on the light theme.
     *
     * **See:** [Arch Wiki on GTK](https://wiki.archlinux.org/index.php/GTK#Themes)
     */
    private enum class DefaultGtkTheme(val info : GtkTheme) {
        DARK(GtkTheme("Adwaita-dark")),
        LIGHT(GtkTheme("Adwaita")),
        HIGH_CONTRAST(GtkTheme("HighContrast")),
    }

    private const val DEFAULT_GUESS_LIGHT_AND_DARK_THEMES = true

    @JvmField var darkGtkTheme = DefaultGtkTheme.DARK.info
    @JvmField var lightGtkTheme = DefaultGtkTheme.LIGHT.info
    @JvmField var highContrastGtkTheme = DefaultGtkTheme.HIGH_CONTRAST.info

    @JvmField var guessLightAndDarkThemes = DEFAULT_GUESS_LIGHT_AND_DARK_THEMES

    init {
        if(!GnomeLibrary.get().isLoaded){
            throw IllegalStateException("Gnome library not loaded.")
        }
        group("Gnome Theme") {
            val installedThemes = GnomeThemeUtils.getInstalledThemes()
            val installedGtkThemes = installedThemes.stream().map { t -> GtkTheme(t) }.toList()
            val lafRenderer = GtkTheme::getName
            val lafTransformer = transformerOf(write = ::parseGtkTheme, read = ::readGtkTheme.or(""))

            persistentBooleanProperty(
                description = "Guess light/dark theme based on name",
                value = ::guessLightAndDarkThemes
            )

            persistentChoiceProperty(
                description = "Light GTK Theme",
                value = ::lightGtkTheme,
                transformer = lafTransformer.writeFallback(DefaultGtkTheme.LIGHT.info)
            ) { choices = installedGtkThemes; renderer = lafRenderer }
            persistentChoiceProperty(
                description = "Dark GTK Theme",
                value = ::darkGtkTheme,
                transformer = lafTransformer.writeFallback(DefaultGtkTheme.DARK.info)
            ) { choices = installedGtkThemes; renderer = lafRenderer }
            persistentChoiceProperty(
                description = "High Contrast GTK Theme",
                value = ::highContrastGtkTheme,
                transformer = lafTransformer.writeFallback(DefaultGtkTheme.HIGH_CONTRAST.info)
            ) { choices = installedGtkThemes; renderer = lafRenderer }
        }
    }

    private fun readGtkTheme(info: GtkTheme): String = info.name

    private fun parseGtkTheme(name: String?): GtkTheme? = GtkTheme(name)
}
