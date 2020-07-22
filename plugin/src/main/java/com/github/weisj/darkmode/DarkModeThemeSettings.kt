package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.IntelliJLookAndFeelInfo
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.options.Scheme
import javax.swing.UIManager

/**
 * Workaround for the fact that auto service currently doesn't work with singleton objects.
 * https://github.com/google/auto/issues/785
 */
@AutoService(SettingsContainer::class)
class GeneralThemeSettingsProxy : SettingsContainer by GeneralThemeSettings

object GeneralThemeSettings : DefaultSettingsContainer() {

    private enum class DefaultLaf(val info : UIManager.LookAndFeelInfo) {
        DARK(DarculaLookAndFeelInfo()),
        LIGHT(searchLaf("IntelliJ Light") ?: IntelliJLookAndFeelInfo()),
        HIGH_CONTRAST(searchLaf("High Contrast") ?: IntelliJLookAndFeelInfo())
    }

    private enum class DefaultScheme(val scheme: EditorColorsScheme) {
        LIGHT(searchScheme("IntelliJ Light", EditorColorsScheme.DEFAULT_SCHEME_NAME)),
        DARK(searchScheme("Darcula")),
        /*
         *  Note: The small c in the second "contrast" is the cyrillic character `с`.
         * Some versions of IDEA use the incorrect character. We simply search for both version.
         */
        HIGH_CONTRAST(searchScheme("High contrast", "High сontrast"))
    }

    private const val DEFAULT_CHECK_HIGH_CONTRAST = true

    var darkTheme = DefaultLaf.DARK.info
    var lightTheme = DefaultLaf.LIGHT.info
    var highContrastTheme = DefaultLaf.HIGH_CONTRAST.info

    var lightCodeScheme = DefaultScheme.LIGHT.scheme
    var darkCodeScheme = DefaultScheme.DARK.scheme
    var highContrastCodeScheme = DefaultScheme.HIGH_CONTRAST.scheme

    var checkHighContrast = DEFAULT_CHECK_HIGH_CONTRAST

    init {
        group("IDE Theme") {
            val installedLafs = LafManager.getInstance().installedLookAndFeels.asList()
            val lafRenderer = UIManager.LookAndFeelInfo::getName
            val lafTransformer = transformerOf(write = ::parseLaf, read = ::readLaf.or(""))

            persistentChoiceProperty(
                description = "Light",
                value = ::lightTheme,
                transformer = lafTransformer.writeFallback(DefaultLaf.LIGHT.info)
            ) { choices = installedLafs; renderer = lafRenderer }
            persistentChoiceProperty(
                description = "Dark",
                value = ::darkTheme,
                transformer = lafTransformer.writeFallback(DefaultLaf.DARK.info)
            ) { choices = installedLafs; renderer = lafRenderer }
            persistentChoiceProperty(
                description = "High Contrast",
                value = ::highContrastTheme,
                transformer = lafTransformer.writeFallback(DefaultLaf.HIGH_CONTRAST.info)
            ) { choices = installedLafs; renderer = lafRenderer }
        }

        group("Editor Theme") {
            val installedSchemes = EditorColorsManager.getInstance().allSchemes.asList()
            val schemeRenderer = EditorColorsScheme::getDisplayName
            val schemeTransformer = transformerOf(write = ::parseScheme, read = ::readScheme.or(""))

            persistentChoiceProperty(
                description = "Light",
                value = ::lightCodeScheme,
                transformer = schemeTransformer.writeFallback(DefaultScheme.LIGHT.scheme)
            ) { choices = installedSchemes; renderer = schemeRenderer }
            persistentChoiceProperty(
                description = "Dark",
                value = ::darkCodeScheme,
                transformer = schemeTransformer.writeFallback(DefaultScheme.DARK.scheme)
            ) { choices = installedSchemes; renderer = schemeRenderer }
            persistentChoiceProperty(
                description = "High Contrast",
                value = ::highContrastCodeScheme,
                transformer = schemeTransformer.writeFallback(DefaultScheme.HIGH_CONTRAST.scheme)
            ) { choices = installedSchemes; renderer = schemeRenderer }
        }

        unnamedGroup {
            persistentBooleanProperty(
                description = "Check for high contrast",
                value = ::checkHighContrast
            )
        }
    }

    private fun readLaf(info: UIManager.LookAndFeelInfo): String = "${info.className} ${info.name}"

    private fun readScheme(scheme: EditorColorsScheme): String = scheme.name

    private fun parseLaf(name: String?): UIManager.LookAndFeelInfo? = name?.toPair(' ')?.let {
        searchLaf(it.first, it.second)
    }

    private fun parseScheme(name: String?): EditorColorsScheme? =
        EditorColorsManager.getInstance().allSchemes.firstOrNull { it.name == name }

    /**
     * Search for a given editor scheme.
     * Schemes may or may not be present in editable form and vice versa.
     * First try to match the name directly.
     * If this doesn't succeed try again with the editable version of the name.
     *
     * Uses the current scheme as a fallback.
     */
    private fun searchScheme(vararg names: String): EditorColorsScheme {
        return EditorColorsManager.getInstance().run {
            names.mapNotNull { name ->
                allSchemes.firstOrNull { it.name == name }
                    ?: allSchemes.firstOrNull { it.name == "${Scheme.EDITABLE_COPY_PREFIX}${name}" }
            }.firstOrNull() ?: globalScheme
        }
    }

    /**
     * Search for a given LookAndFeelInfo.
     * The name has to match. If the className isn't empty it also has to match.
     */
    private fun searchLaf(name: String, className: String = ""): UIManager.LookAndFeelInfo? {
        return LafManager.getInstance().installedLookAndFeels.firstOrNull {
            it.name.toLowerCase() == name.toLowerCase() && (className.isEmpty() || it.className == className)
        }
    }
}
