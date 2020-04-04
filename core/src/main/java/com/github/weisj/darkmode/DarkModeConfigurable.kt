package com.github.weisj.darkmode

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.panel
import javax.swing.UIManager

class DarkModeConfigurable(private val lafManager: LafManager) : BoundConfigurable("Auto Dark Mode") {

    override fun createPanel(): DialogPanel {
        val options = ServiceManager.getService(AutoDarkModeThemes::class.java)
        val lookAndFeels = lafManager.installedLookAndFeels.asList()
        val renderer = SimpleListCellRenderer.create("") { obj: UIManager.LookAndFeelInfo -> obj.name }
        return panel {
            row("Light Theme:") {
                comboBox(CollectionComboBoxModel(lookAndFeels), options::light, renderer = renderer)
            }
            row("Dark Theme:") {
                comboBox(CollectionComboBoxModel(lookAndFeels), options::dark, renderer = renderer)
            }
            row("High Contrast Theme:") {
                comboBox(CollectionComboBoxModel(lookAndFeels), options::highContrast, renderer = renderer)
            }
        }
    }
}
