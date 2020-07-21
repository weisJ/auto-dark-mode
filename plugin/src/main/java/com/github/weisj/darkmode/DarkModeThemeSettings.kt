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

typealias LafInfo = UIManager.LookAndFeelInfo

object GeneralThemeSettings : DefaultSettingsContainer() {

    private val DEFAULT_DARK_THEME: LafInfo = DarculaLookAndFeelInfo()
    private val DEFAULT_LIGHT_THEME: LafInfo = searchLaf("IntelliJ Light") ?: IntelliJLookAndFeelInfo()
    private val DEFAULT_HIGH_CONTRAST_THEME: LafInfo = searchLaf("High Contrast") ?: IntelliJLookAndFeelInfo()

    private val DEFAULT_LIGHT_SCHEME: EditorColorsScheme =
        searchScheme("IntelliJ Light", EditorColorsScheme.DEFAULT_SCHEME_NAME)
    private val DEFAULT_DARK_SCHEME: EditorColorsScheme =
        searchScheme("Darcula")
    private val DEFAULT_HIGH_CONTRAST_SCHEME: EditorColorsScheme =
        /*
         *  Note: The small c in the second "contrast" is the cyrillic character `с`.
         * Some versions of IDEA use the incorrect character. We simply search for both version.
         */
        searchScheme("High contrast", "High сontrast")

    private const val DEFAULT_CHECK_HIGH_CONTRAST = true

    var darkTheme = DEFAULT_DARK_THEME
    var lightTheme = DEFAULT_LIGHT_THEME
    var highContrastTheme = DEFAULT_HIGH_CONTRAST_THEME

    var lightCodeScheme = DEFAULT_LIGHT_SCHEME
    var darkCodeScheme = DEFAULT_DARK_SCHEME
    var highContrastCodeScheme = DEFAULT_HIGH_CONTRAST_SCHEME

    var checkHighContrast = DEFAULT_CHECK_HIGH_CONTRAST

    init {
        group("IDE Theme") {
            val installedLafs = LafManager.getInstance().installedLookAndFeels.asList()
            val lafRenderer = { obj: LafInfo -> obj.name }
            val createLafTransformer = { fallback: LafInfo -> Transformer(parseLaf(fallback), ::writeLaf) }

            persistentChoiceProperty(
                description = "Light",
                value = ::lightTheme,
                transformer = createLafTransformer(DEFAULT_LIGHT_THEME)
            ) { choices = installedLafs; renderer = lafRenderer }
            persistentChoiceProperty(
                description = "Dark",
                value = ::darkTheme,
                transformer = createLafTransformer(DEFAULT_DARK_THEME)
            ) { choices = installedLafs; renderer = lafRenderer }
            persistentChoiceProperty(
                description = "High Contrast",
                value = ::highContrastTheme,
                transformer = createLafTransformer(DEFAULT_HIGH_CONTRAST_THEME)
            ) { choices = installedLafs; renderer = lafRenderer }
        }

        group("Editor Theme") {
            val installedSchemes = EditorColorsManager.getInstance().allSchemes.asList()
            val schemeRenderer = { obj: EditorColorsScheme -> obj.displayName }
            val createSchemeTransformer =
                { fallback: EditorColorsScheme -> Transformer(parseScheme(fallback), ::writeScheme) }

            persistentChoiceProperty(
                description = "Light",
                value = ::lightCodeScheme,
                transformer = createSchemeTransformer(DEFAULT_LIGHT_SCHEME)
            ) { choices = installedSchemes; renderer = schemeRenderer }
            persistentChoiceProperty(
                description = "Dark",
                value = ::darkCodeScheme,
                transformer = createSchemeTransformer(DEFAULT_DARK_SCHEME)
            ) { choices = installedSchemes; renderer = schemeRenderer }
            persistentChoiceProperty(
                description = "High Contrast",
                value = ::highContrastCodeScheme,
                transformer = createSchemeTransformer(DEFAULT_HIGH_CONTRAST_SCHEME)
            ) { choices = installedSchemes; renderer = schemeRenderer }
        }

        unnamed {
            persistentBooleanProperty(
                description = "Check for high contrast",
                value = ::checkHighContrast
            )
        }
    }

    override fun isEnabled(): Boolean = true

    /**
     * Returns a parser function with the given fallback.
     */
    private fun parseLaf(fallback: LafInfo): (String) -> LafInfo {
        return { s -> s.toPair('|')?.let { searchLaf(it.first, it.second) } ?: fallback }
    }

    private fun writeLaf(info: LafInfo): String = "${info.className} ${info.name}"

    /**
     * Returns a parser function with the given fallback.
     */
    private fun parseScheme(fallback: EditorColorsScheme): (String) -> EditorColorsScheme {
        return { s -> getScheme(s) ?: fallback }
    }

    private fun writeScheme(scheme: EditorColorsScheme): String = scheme.name

    private fun getScheme(name: String): EditorColorsScheme? =
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
     * Search for a given LookAndFeelInfo. The name has to match. If the className isn't empty it also has to match.
     */
    private fun searchLaf(name: String, className: String = ""): LafInfo? {
        return LafManager.getInstance().installedLookAndFeels.firstOrNull {
            it.name.toLowerCase() == name.toLowerCase() && (className.isEmpty() || it.className == className)
        }
    }
}
