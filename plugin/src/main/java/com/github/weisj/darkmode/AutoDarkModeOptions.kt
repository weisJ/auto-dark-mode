package com.github.weisj.darkmode

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.IntelliJLookAndFeelInfo
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorColorsScheme.DEFAULT_SCHEME_NAME
import com.intellij.openapi.options.Scheme.EDITABLE_COPY_PREFIX
import javax.swing.UIManager

@State(name = "AutoDarkMode", storages = [Storage("auto-dark-mode.xml", roamingType = RoamingType.PER_OS)])
class AutoDarkModeOptions : PersistentStateComponent<AutoDarkModeOptions.State> {

    @Volatile
    var darkTheme: UIManager.LookAndFeelInfo = DEFAULT_DARK_THEME

    @Volatile
    var lightTheme: UIManager.LookAndFeelInfo = DEFAULT_LIGHT_THEME

    @Volatile
    var highContrastTheme: UIManager.LookAndFeelInfo = DEFAULT_HIGH_CONTRAST_THEME

    @Volatile
    var lightCodeScheme: EditorColorsScheme = DEFAULT_LIGHT_SCHEME

    @Volatile
    var darkCodeScheme: EditorColorsScheme = DEFAULT_DARK_SCHEME

    @Volatile
    var highContrastCodeScheme: EditorColorsScheme = DEFAULT_HIGH_CONTRAST_SCHEME

    @Volatile
    var checkHighContrast: Boolean = DEFAULT_CHECK_HIGH_CONTRAST

    override fun getState(): State? {
        return State(
            darkTheme.name, darkTheme.className,
            lightTheme.name, lightTheme.className,
            highContrastTheme.name, highContrastTheme.className,
            lightCodeScheme.name, darkCodeScheme.name,
            highContrastCodeScheme.name, checkHighContrast
        )
    }

    override fun loadState(state: State) {
        val lafManager = LafManager.getInstance()
        darkTheme = lafManager.installedLookAndFeels
            .first { it.name == state.darkName && it.className == state.darkClassName }
            ?: DEFAULT_DARK_THEME
        lightTheme = lafManager.installedLookAndFeels
            .first { it.name == state.lightName && it.className == state.lightClassName }
            ?: DEFAULT_LIGHT_THEME
        highContrastTheme = lafManager.installedLookAndFeels
            .first { it.name == state.highContrastName && it.className == state.highContrastNameClassName }
            ?: DEFAULT_HIGH_CONTRAST_THEME

        val schemes = EditorColorsManager.getInstance().allSchemes
        lightCodeScheme = schemes
            .first { it.name == state.lightSchemeName }
            ?: DEFAULT_LIGHT_SCHEME
        darkCodeScheme = schemes
            .first { it.name == state.darkSchemeName }
            ?: DEFAULT_DARK_SCHEME
        highContrastCodeScheme = schemes
            .first { it.name == state.highContrastSchemeName }
            ?: DEFAULT_HIGH_CONTRAST_SCHEME

        checkHighContrast = state.checkHighContrast ?: DEFAULT_CHECK_HIGH_CONTRAST
    }

    data class State(
        var darkName: String? = DEFAULT_DARK_THEME.name,
        var darkClassName: String? = DEFAULT_DARK_THEME.className,
        var lightName: String? = DEFAULT_LIGHT_THEME.name,
        var lightClassName: String? = DEFAULT_LIGHT_THEME.className,
        var highContrastName: String? = DEFAULT_HIGH_CONTRAST_THEME.name,
        var highContrastNameClassName: String? = DEFAULT_HIGH_CONTRAST_THEME.className,
        var lightSchemeName: String? = DEFAULT_LIGHT_SCHEME.name,
        var darkSchemeName: String? = DEFAULT_DARK_SCHEME.name,
        var highContrastSchemeName: String? = DEFAULT_HIGH_CONTRAST_SCHEME.name,
        var checkHighContrast: Boolean? = DEFAULT_CHECK_HIGH_CONTRAST
    )

    companion object {

        private fun searchScheme(vararg names: String): EditorColorsScheme = EditorColorsManager.getInstance().run {
            names.mapNotNull { name ->
                allSchemes.find { it.name == name } ?: allSchemes.find { it.name == "${EDITABLE_COPY_PREFIX}${name}" }
            }.firstOrNull() ?: globalScheme
        }

        val DEFAULT_DARK_THEME: UIManager.LookAndFeelInfo = DarculaLookAndFeelInfo()
        val DEFAULT_LIGHT_THEME: UIManager.LookAndFeelInfo = IntelliJLookAndFeelInfo()
        val DEFAULT_HIGH_CONTRAST_THEME: UIManager.LookAndFeelInfo = LafManager.getInstance()
            .installedLookAndFeels.find { it.name.toLowerCase() == "high contrast" }
            ?: IntelliJLookAndFeelInfo()

        val DEFAULT_LIGHT_SCHEME: EditorColorsScheme = searchScheme("IntelliJ Light", DEFAULT_SCHEME_NAME)
        val DEFAULT_DARK_SCHEME: EditorColorsScheme = searchScheme("Darcula")

        // Note: The small c in the second contrast is the cyrillic letter `с`.
        val DEFAULT_HIGH_CONTRAST_SCHEME: EditorColorsScheme = searchScheme("High contrast", "High сontrast")

        const val DEFAULT_CHECK_HIGH_CONTRAST = true
    }
}
