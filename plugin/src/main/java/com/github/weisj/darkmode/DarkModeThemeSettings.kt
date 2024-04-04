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
@file:Suppress("UnstableApiUsage")

package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeExportableBean
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.options.Scheme
import com.intellij.ui.ExperimentalUI
import javax.swing.UIDefaults

@AutoService(SettingsContainerProvider::class)
class GeneralThemeSettingsProvider : SingletonSettingsContainerProvider({ GeneralThemeSettings })

@Suppress("kotlin:S1192")
object GeneralThemeSettings : DefaultSettingsContainer(identifier = "general_settings") {

    /**
     * Default values for the LookAndFeel which are guaranteed to be bundled with any
     * IntelliJ based product.
     */
    private enum class DefaultLaf(val info: UIThemeLookAndFeelInfo) {
        DARK(searchLaf(LafFallback.Dark, "ExperimentalDark", "Darcula")),
        LIGHT(searchLaf(LafFallback.Light, "ExperimentalLight", "JetBrainsLightTheme")),
        HIGH_CONTRAST(searchLaf(LafFallback.HighContrast , "JetBrainsHighContrastTheme"))
    }

    /**
     * Default values for the code scheme which are guaranteed to be bundled with any
     * IntelliJ based product.
     */
    private enum class DefaultScheme(val scheme: EditorColorsScheme) {
        LIGHT(searchScheme("IntelliJ Light", EditorColorsScheme.DEFAULT_SCHEME_NAME)),
        DARK(searchScheme("Darcula")),

        /*
         * Note: The small c in the second "contrast" is the cyrillic character `с`.
         * Some versions of IDEA use the incorrect character. We simply search for both version.
         */
        HIGH_CONTRAST(searchScheme("High contrast", "High сontrast"))
    }

    private const val DEFAULT_CHANGE_IDE_THEME = true
    private const val DEFAULT_CHANGE_EDITOR_THEME = true
    private const val DEFAULT_CHECK_HIGH_CONTRAST = true

    var darkTheme = DefaultLaf.DARK.info
    var lightTheme = DefaultLaf.LIGHT.info
    var highContrastTheme = DefaultLaf.HIGH_CONTRAST.info

    var lightCodeScheme = DefaultScheme.LIGHT.scheme
    var darkCodeScheme = DefaultScheme.DARK.scheme
    var highContrastCodeScheme = DefaultScheme.HIGH_CONTRAST.scheme

    var changeIdeTheme = DEFAULT_CHANGE_IDE_THEME
    var changeEditorTheme = DEFAULT_CHANGE_EDITOR_THEME
    var checkHighContrast = DEFAULT_CHECK_HIGH_CONTRAST

    init {
        group("IDE Theme") {
            val installedLafsProvider = { LafManager.getInstance().installedThemes.toList() }
            val lafRenderer = UIThemeLookAndFeelInfo::name
            val lafTransformer = transformerOf(write = ::parseLaf, read = ::readLaf.or(""))

            persistentBooleanProperty(
                description = "Change IDE Theme",
                value = ::changeIdeTheme
            )

            group {
                activeIf(::changeIdeTheme.isTrue())

                persistentChoiceProperty(
                    description = "Light",
                    value = ::lightTheme,
                    transformer = lafTransformer.writeFallback(DefaultLaf.LIGHT.info)
                ) { choicesProvider = installedLafsProvider; renderer = lafRenderer; }
                persistentChoiceProperty(
                    description = "Dark",
                    value = ::darkTheme,
                    transformer = lafTransformer.writeFallback(DefaultLaf.DARK.info)
                ) { choicesProvider = installedLafsProvider; renderer = lafRenderer }
                persistentChoiceProperty(
                    description = "High Contrast",
                    value = ::highContrastTheme,
                    transformer = lafTransformer.writeFallback(DefaultLaf.HIGH_CONTRAST.info)
                ) {
                    choicesProvider = installedLafsProvider; renderer = lafRenderer
                    activeIf(::checkHighContrast.isTrue())
                }
            }
        }

        group("Editor Theme") {
            val installedSchemesProvider = { EditorColorsManager.getInstance().allSchemes.asList() }
            val schemeRenderer = EditorColorsScheme::getDisplayName
            val schemeTransformer = transformerOf(write = ::parseScheme, read = ::readScheme.or(""))

            persistentBooleanProperty(
                description = "Change Editor Theme",
                value = ::changeEditorTheme
            )

            group {
                activeIf(::changeEditorTheme.isTrue())

                persistentChoiceProperty(
                    description = "Light",
                    value = ::lightCodeScheme,
                    transformer = schemeTransformer.writeFallback(DefaultScheme.LIGHT.scheme)
                ) { choicesProvider = installedSchemesProvider; renderer = schemeRenderer }
                persistentChoiceProperty(
                    description = "Dark",
                    value = ::darkCodeScheme,
                    transformer = schemeTransformer.writeFallback(DefaultScheme.DARK.scheme)
                ) { choicesProvider = installedSchemesProvider; renderer = schemeRenderer }
                persistentChoiceProperty(
                    description = "High Contrast",
                    value = ::highContrastCodeScheme,
                    transformer = schemeTransformer.writeFallback(DefaultScheme.HIGH_CONTRAST.scheme)
                ) {
                    choicesProvider = installedSchemesProvider; renderer = schemeRenderer
                    activeIf(::checkHighContrast.isTrue())
                }
            }
        }

        group("Other") {
            persistentBooleanProperty(
                description = "Check for high contrast",
                value = ::checkHighContrast
            )
        }
    }

    private fun readLaf(info: UIThemeLookAndFeelInfo): String = info.id

    private fun readScheme(scheme: EditorColorsScheme): String = scheme.name

    private fun parseLaf(id: String?): UIThemeLookAndFeelInfo? = id?.let {
        it.toPair(' ')?.let { p -> searchLaf(LafFallback.Current, p.first) } ?: searchLaf(LafFallback.Current, it)
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
            names.firstNotNullOfOrNull { name ->
                allSchemes.firstOrNull { it.name == name }
                    ?: allSchemes.firstOrNull { it.name == "${Scheme.EDITABLE_COPY_PREFIX}$name" }
            } ?: globalScheme
        }
    }

    enum class LafFallback {
        Dark,
        Light,
        HighContrast,
        Current
    }

    /**
     * Search for a given LookAndFeelInfo.
     * The name has to match. If the className isn't empty it also has to match.
     */
    private fun searchLaf(type: LafFallback, vararg ids: String): UIThemeLookAndFeelInfo {
        return LafManager.getInstance()?.run {
            val fallback = when (type) {
                LafFallback.Dark -> defaultDarkLaf ?: currentUIThemeLookAndFeel
                LafFallback.Light -> defaultLightLaf ?: currentUIThemeLookAndFeel
                LafFallback.HighContrast, LafFallback.Current -> currentUIThemeLookAndFeel
            }
            val experimental = ExperimentalUI.isNewUI()
            ids.firstNotNullOfOrNull { id ->
                if (!experimental && id.startsWith("Experimental")) {
                    return@firstNotNullOfOrNull null
                }
                return@firstNotNullOfOrNull installedThemes.firstOrNull {
                    it.id.equals(id, ignoreCase = true)
                }
            } ?: fallback
        } ?: HeadlessUIThemeLookAndFeelInfo()
    }
}

private class HeadlessUIThemeLookAndFeelInfo : UIThemeLookAndFeelInfo {
    override val author: String?
        get() = null
    override val editorSchemeId: String?
        get() = null
    override val id: String
        get() = "headless"
    override val isDark: Boolean
        get() = false
    override val isInitialized: Boolean
        get() = false
    override val name: String
        get() = "Headless"
    override val providerClassLoader: ClassLoader
        get() = this.javaClass.classLoader

    override fun describe(): UIThemeExportableBean {
        return UIThemeExportableBean(
            emptyMap(),
            emptyMap(),
            emptyMap(),
            emptyMap()
        )
    }

    override fun dispose() {}

    override fun installEditorScheme(previousSchemeForLaf: EditorColorsScheme?) {}

    override fun installTheme(defaults: UIDefaults) {}

}
