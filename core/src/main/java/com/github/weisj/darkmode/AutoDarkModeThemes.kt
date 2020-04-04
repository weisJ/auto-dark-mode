package com.github.weisj.darkmode

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.IntelliJLookAndFeelInfo
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import javax.swing.UIManager

@State(name = "AutoDarkMode", storages = [Storage("auto-dark-mode.xml", roamingType = RoamingType.PER_OS)])
class AutoDarkModeThemes : PersistentStateComponent<AutoDarkModeThemes.State> {

    @Volatile
    var dark: UIManager.LookAndFeelInfo = DEFAULT_DARK_THEME

    @Volatile
    var light: UIManager.LookAndFeelInfo = DEFAULT_LIGHT_THEME

    @Volatile
    var highContrast: UIManager.LookAndFeelInfo = DEFAULT_HIGH_CONTRAST_THEME

    override fun getState(): State? {
        return State(
            dark.name, dark.className,
            light.name, light.className,
            highContrast.name, highContrast.className
        )
    }

    override fun loadState(state: State) {
        val lafManager = LafManager.getInstance()
        dark = lafManager.installedLookAndFeels
            .find { it.name == state.darkName && it.className == state.darkClassName }
            ?: DEFAULT_DARK_THEME
        light = lafManager.installedLookAndFeels
            .first { it.name == state.lightName && it.className == state.lightClassName }
            ?: DEFAULT_LIGHT_THEME
        highContrast = lafManager.installedLookAndFeels
            .first { it.name == state.highContrastName && it.className == state.highContrastNameClassName }
            ?: DEFAULT_HIGH_CONTRAST_THEME
    }

    data class State(
        var darkName: String? = DEFAULT_DARK_THEME.name,
        var darkClassName: String? = DEFAULT_DARK_THEME.className,
        var lightName: String? = DEFAULT_LIGHT_THEME.name,
        var lightClassName: String? = DEFAULT_LIGHT_THEME.className,
        var highContrastName: String? = DEFAULT_HIGH_CONTRAST_THEME.name,
        var highContrastNameClassName: String? = DEFAULT_HIGH_CONTRAST_THEME.className
    )

    companion object {
        val DEFAULT_DARK_THEME: UIManager.LookAndFeelInfo = DarculaLookAndFeelInfo()
        val DEFAULT_LIGHT_THEME: UIManager.LookAndFeelInfo = IntelliJLookAndFeelInfo()
        val DEFAULT_HIGH_CONTRAST_THEME: UIManager.LookAndFeelInfo = IntelliJLookAndFeelInfo()
    }
}
