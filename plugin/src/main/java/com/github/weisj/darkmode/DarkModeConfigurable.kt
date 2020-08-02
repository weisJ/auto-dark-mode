package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.*
import com.intellij.util.castSafelyTo
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JToggleButton

class DarkModeConfigurable : BoundConfigurable(SETTINGS_TITLE) {

    override fun createPanel(): DialogPanel {
        val options = ServiceManager.getService(AutoDarkModeOptions::class.java)
        val rowMap: MutableMap<ValueProperty<*>, Row> = mutableMapOf()

        return panel {
            options.containers.forEach { container ->
                container.namedGroups.forEach { addGroup(it, rowMap) }
                addGroup(container.unnamedGroup, UNNAMED_GROUP_TITLE, rowMap)
            }
        }
    }

    private fun Row.addProperty(valueProp: ValueProperty<Any>, rowMap: MutableMap<ValueProperty<*>, Row>) {
        val choiceProperty = valueProp.castSafelyTo<ChoiceProperty<Any, Any>>()
        val property = valueProp.effectiveProperty
        val rowName = if (property.get() is Boolean) "" else valueProp.description
        lateinit var comp: JComponent
        rowMap[valueProp] = maybeNamedRow(rowName) {
            comp = when {
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
                else -> throw IllegalArgumentException("Not yet implemented")
            }
        }
        val propertyController = valueProp.castSafelyTo<PropertyController<Any>>()
        if (propertyController != null) {
            comp.asPredicate(propertyController.predicate)?.let { predicate ->
                propertyController.controlled.forEach { controlledProperty ->
                    rowMap[controlledProperty]?.enableIf(predicate)
                    predicate.addListener { controlledProperty.active = it }
                }
            }
        }
    }

    private fun JComponent.asPredicate(predicate: (Any?) -> Boolean): ComponentPredicate? {
        return when (this) {
            is JComboBox<*> -> ComboBoxPredicate(this) { predicate(it) }
            is JToggleButton -> object : ComponentPredicate() {
                override fun invoke(): Boolean = predicate(isSelected)

                override fun addListener(listener: (Boolean) -> Unit) {
                    addChangeListener { listener(predicate(isSelected)) }
                }
            }
            else -> null
        }
    }

    private fun LayoutBuilder.addGroup(
        properties: SettingsGroup,
        name: String?,
        rowMap: MutableMap<ValueProperty<*>, Row>
    ) {
        maybeTitledRow(name) { properties.forEach { addProperty(it, rowMap) } }
    }

    private fun LayoutBuilder.addGroup(group: NamedSettingsGroup, rowMap: MutableMap<ValueProperty<*>, Row>) =
        addGroup(group, group.name, rowMap)

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
