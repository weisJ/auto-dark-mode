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

    private val DEFAULT_DARK_THEME: UIManager.LookAndFeelInfo =
        DarculaLookAndFeelInfo()
    private val DEFAULT_LIGHT_THEME: UIManager.LookAndFeelInfo =
        searchLaf("IntelliJ Light") ?: IntelliJLookAndFeelInfo()
    private val DEFAULT_HIGH_CONTRAST_THEME: UIManager.LookAndFeelInfo =
        searchLaf("High Contrast") ?: IntelliJLookAndFeelInfo()

    private val DEFAULT_LIGHT_SCHEME: EditorColorsScheme =
        searchScheme("IntelliJ Light", EditorColorsScheme.DEFAULT_SCHEME_NAME)
    private val DEFAULT_DARK_SCHEME: EditorColorsScheme =
        searchScheme("Darcula")
    private val DEFAULT_HIGH_CONTRAST_SCHEME: EditorColorsScheme =
        // Note: The small c in the second contrast is the cyrillic letter `с`.
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
            val lafRenderer = { obj: UIManager.LookAndFeelInfo -> obj.name }

            choiceProperty("Light", ::lightTheme, parseLaf(DEFAULT_LIGHT_THEME), ::writeLaf) {
                options = installedLafs; renderer = lafRenderer
            }
            choiceProperty("Dark", ::darkTheme, parseLaf(DEFAULT_DARK_THEME), ::writeLaf) {
                options = installedLafs; renderer = lafRenderer
            }
            choiceProperty("High Contrast", ::highContrastTheme, parseLaf(DEFAULT_HIGH_CONTRAST_THEME), ::writeLaf) {
                options = installedLafs; renderer = lafRenderer
            }
        }

        group("Editor Theme") {
            val installedSchemes = EditorColorsManager.getInstance().allSchemes.asList()
            val schemeRenderer = { obj: EditorColorsScheme -> obj.displayName }
            choiceProperty("Light", ::lightCodeScheme, parseScheme(DEFAULT_LIGHT_SCHEME), ::writeScheme) {
                options = installedSchemes; renderer = schemeRenderer
            }
            choiceProperty("Dark", ::darkCodeScheme, parseScheme(DEFAULT_DARK_SCHEME), ::writeScheme) {
                options = installedSchemes; renderer = schemeRenderer
            }
            choiceProperty(
                "High Contrast", ::highContrastCodeScheme,
                parseScheme(DEFAULT_HIGH_CONTRAST_SCHEME), ::writeScheme
            ) {
                options = installedSchemes; renderer = schemeRenderer
            }
        }

        booleanProperty("Check for high contrast", ::checkHighContrast)
    }

    private fun parseLaf(fallback: UIManager.LookAndFeelInfo): (String) -> UIManager.LookAndFeelInfo {
        return { s ->
            s.split(delimiters = *charArrayOf('|'), limit = 2).let {
                val name = it[0]
                val className = it[1]
                searchLaf(name, className) ?: fallback
            }
        }
    }

    private fun writeLaf(info: UIManager.LookAndFeelInfo): String = "${info.name}|${info.className}"

    private fun parseScheme(fallback: EditorColorsScheme): (String) -> EditorColorsScheme {
        return { s -> getScheme(s) ?: fallback }
    }

    private fun writeScheme(scheme: EditorColorsScheme): String = scheme.name

    private fun getScheme(name: String): EditorColorsScheme? =
        EditorColorsManager.getInstance().allSchemes.firstOrNull { it.name == name }

    private fun searchScheme(vararg names: String): EditorColorsScheme {
        return EditorColorsManager.getInstance().run {
            names.mapNotNull { name ->
                allSchemes.firstOrNull { it.name == name }
                    ?: allSchemes.firstOrNull { it.name == "${Scheme.EDITABLE_COPY_PREFIX}${name}" }
            }.firstOrNull() ?: globalScheme
        }
    }

    private fun searchLaf(name: String, className: String = ""): UIManager.LookAndFeelInfo? {
        return LafManager.getInstance().installedLookAndFeels.firstOrNull {
            it.name.toLowerCase() == name.toLowerCase() && (className.isEmpty() || it.className == className)
        }
    }
}
