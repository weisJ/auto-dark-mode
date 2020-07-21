package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.castSafelyTo

class DarkModeConfigurable : BoundConfigurable("Auto Dark Mode") {

    override fun createPanel(): DialogPanel {
        val options = ServiceManager.getService(AutoDarkModeOptions::class.java)

        return panel {
            options.containers.flatMap { container ->
                container.namedGroups.forEach { addGroup(it) }
                container.unnamedGroup
            }.let { addGroup(it as SettingsGroup, "Other") }
        }
    }

    private fun Row.addProperty(valueProp: ValueProperty<Any>) {
        valueProp.castSafelyTo<ChoiceProperty<Any, Any>>()?.let {
            val propertyRenderer = SimpleListCellRenderer.create("", it.renderer)
            row(valueProp.description) {
                comboBox(CollectionComboBoxModel(it.choices), it::choiceValue, renderer = propertyRenderer)
            }
            return
        }
        val value = valueProp.asPersistent()?.backingValue?:valueProp.value
        val property = valueProp.asPersistent()?.let { it::backingValue }?: valueProp::value
        when (value) {
            is Boolean -> row { checkBox(valueProp.description, property.withType()!!) }
            is String -> row(valueProp.description) { textField(property.withType()!!) }
            else -> throw IllegalArgumentException("Not yet implemented")
        }
    }

    private fun LayoutBuilder.addGroup(properties: SettingsGroup, name: String) {
        titledRow(name) { properties.forEach { addProperty(it) } }
    }

    private fun LayoutBuilder.addGroup(group: NamedSettingsGroup) = addGroup(group, group.name)

    override fun apply() {
        super.apply()
        ServiceManager.getService(AutoDarkMode::class.java).onSettingsChange()
    }
}
