package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.RowBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.castSafelyTo
import javax.swing.JComboBox
import javax.swing.JComponent

class DarkModeConfigurable : BoundConfigurable(SETTINGS_TITLE) {

    override fun createPanel(): DialogPanel {
        val options = ServiceManager.getService(AutoDarkModeOptions::class.java)

        return panel {
            options.containers.forEach { container ->
                container.subgroups.forEach { addGroup(it) }
                addGroup(container.unnamedGroup, UNNAMED_GROUP_TITLE)
            }
        }
    }

    private fun RowBuilder.addGroup(group: NamedSettingsGroup) = addGroup(group, group.name)

    private fun RowBuilder.addGroup(
        properties: SettingsGroup,
        name: String?
    ) {
        maybeTitledRow(name) {
            properties.forEach { addProperty(it) }
            properties.subgroups.forEach { group ->
                if (group.name.isEmpty()) {
                    group.forEach { addProperty(it) }
                } else {
                    addGroup(group)
                }
            }
        }
    }

    private fun Row.addProperty(valueProp: ValueProperty<Any>) {
        val choiceProperty = valueProp.castSafelyTo<ChoiceProperty<Any, Any>>()
        val property = valueProp.effectiveProperty
        val rowName = if (property.get() is Boolean) "" else valueProp.description
        maybeNamedRow(rowName) {
            val comp: JComponent = when {
                choiceProperty != null -> comboBox(
                    CollectionComboBoxModel(choiceProperty.choices),
                    choiceProperty::choiceValue,
                    renderer = SimpleListCellRenderer.create<Any>("", choiceProperty.renderer)
                ).component
                property.get() is Boolean -> {
                    checkBox(valueProp.description, property.withType()!!).component
                }
                property.get() is String -> {
                    textField(property.withType()!!).component
                }
                else -> throw IllegalArgumentException("Not yet implemented!")
            }
            comp.addPreviewListener { valueProp.effective<Any>().preview = it }
        }.also {
            it.enabled = valueProp.activeCondition()
            valueProp.activeCondition.registerListener(Condition::value) { _, _ ->
                it.enabled = valueProp.activeCondition.value
            }
        }
    }

    private fun JComponent.addPreviewListener(listener: (Any) -> Unit) {
        when (this) {
            is JComboBox<*> -> addItemListener { listener(it.itemSelectable.selectedObjects[0]) }
            is JBCheckBox -> addItemListener { listener(isSelected) }
        }
    }

    private fun RowBuilder.maybeTitledRow(name: String?, init: Row.() -> Unit): Row {
        return if (!name.isNullOrEmpty()) titledRow(name, init) else row { init() }
    }

    private fun RowBuilder.maybeNamedRow(name: String?, init: Row.() -> Unit): Row {
        return if (!name.isNullOrEmpty()) row(name) { init() } else row { init() }
    }

    override fun apply() {
        super.apply()
        ServiceManager.getService(AutoDarkMode::class.java).onSettingsChange()
    }

    companion object {
        const val SETTINGS_TITLE: String = "Auto Dark Mode"
        val UNNAMED_GROUP_TITLE: String? = null
    }
}
