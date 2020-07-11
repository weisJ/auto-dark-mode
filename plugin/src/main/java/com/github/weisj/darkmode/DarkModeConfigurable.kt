package com.github.weisj.darkmode

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.InnerCell
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import javax.swing.UIManager
import kotlin.reflect.KMutableProperty0

class DarkModeConfigurable(private val lafManager: LafManager) : BoundConfigurable("Auto Dark Mode") {

    override fun createPanel(): DialogPanel {
        val options = ServiceManager.getService(AutoDarkModeOptions::class.java)

        val lookAndFeels = lafManager.installedLookAndFeels.asList()
        val lafRenderer = SimpleListCellRenderer.create("") { obj: UIManager.LookAndFeelInfo -> obj.name }

        val schemes = EditorColorsManager.getInstance().allSchemes.asList()
        val schemeRenderer = SimpleListCellRenderer.create("") { obj: EditorColorsScheme ->
            StringUtil.trimStart(obj.name, AutoDarkModeOptions.EDITABLE_COPY_PREFIX)
        }

        fun Row.themeMode(
            label: String,
            themeProperty: KMutableProperty0<UIManager.LookAndFeelInfo>,
            schemeProperty: KMutableProperty0<EditorColorsScheme>
        ) {
            row(label) {
                cell {
                    comboBox(CollectionComboBoxModel(lookAndFeels), themeProperty, renderer = lafRenderer)
                    label(" / ")
                    comboBox(CollectionComboBoxModel(schemes), schemeProperty, renderer = schemeRenderer)
                }
            }
        }

        return panel {
            titledRow("IDE Theme / Editor Theme") {
                themeMode("Light Mode:", options::lightTheme, options::lightCodeScheme)
                themeMode("Dark Mode:", options::darkTheme, options::darkCodeScheme)
                themeMode("High Contrast Mode:", options::highContrastTheme, options::highContrastCodeScheme)
            }
            titledRow("Options") {
                row {
                    checkBox("Check for high contrast", options::checkHighContrast)
                }
            }
        }
    }

    private fun Row.cellRow(title: String = "", init: InnerCell.() -> Unit) {
        row(title) {
            cell { this.init() }
        }
    }

    override fun apply() {
        super.apply()
        ServiceManager.getService(AutoDarkMode::class.java).onSettingsChange()
    }
}
