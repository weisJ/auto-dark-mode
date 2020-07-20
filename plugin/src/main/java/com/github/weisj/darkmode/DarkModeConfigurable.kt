package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.ChoiceProperty
import com.github.weisj.darkmode.platform.settings.SettingsGroup
import com.github.weisj.darkmode.platform.settings.ValueProperty
import com.github.weisj.darkmode.platform.settings.withType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel

class DarkModeConfigurable : BoundConfigurable("Auto Dark Mode") {

    override fun createPanel(): DialogPanel {
        val options = ServiceManager.getService(AutoDarkModeOptions::class.java)

        fun Row.addProperty(valueProp: ValueProperty<Any>) {
            if (valueProp is ChoiceProperty) {
                val propertyRenderer = SimpleListCellRenderer.create("", valueProp.renderer)
                row(valueProp.description) {
                    comboBox(
                        CollectionComboBoxModel(valueProp.options),
                        valueProp.property,
                        renderer = propertyRenderer
                    )
                }
                return
            }
            when (valueProp.property.get()) {
                is Boolean -> row { checkBox(valueProp.description, valueProp.property.withType()!!) }
                is String -> row(valueProp.description) { textField(valueProp.property.withType()!!) }
                else -> throw IllegalArgumentException("Not yet implemented")
            }
        }

        fun LayoutBuilder.addGroup(group: SettingsGroup) {
            titledRow(group.name) { group.properties.forEach { addProperty(it) } }
        }

        return panel {
            options.containers.flatMap { container ->
                container.namedGroups.forEach { addGroup(it) }
                container.unnamedGroup.properties
            }.let { unnamed ->
                titledRow("Other") { unnamed.forEach { addProperty(it) } }
            }
        }
    }

    override fun apply() {
        super.apply()
        ServiceManager.getService(AutoDarkMode::class.java).onSettingsChange()
    }
}
