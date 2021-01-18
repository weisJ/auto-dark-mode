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
package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.*
import com.intellij.util.castSafelyTo
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

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
        val effectiveProp = valueProp.effective<Any>()
        val prop = effectiveProp.value
        val rowName = when (prop) {
            is Boolean -> ""
            else -> valueProp.description
        }
        maybeNamedRow(rowName) {
            when {
                choiceProperty != null -> addChoiceProperty(choiceProperty)
                prop is Boolean ->
                    checkBox(valueProp.description, effectiveProp::value.withType()!!)
                        .applyToComponent {
                            addActionListener { effectiveProp.preview = isSelected }
                        }
                prop is String ->
                    textField(effectiveProp::value.withType()!!)
                        .applyToComponent {
                            document.addDocumentListener(
                                DocumentChangeListener {
                                    effectiveProp.preview = text
                                }
                            )
                        }
                prop is Int ->
                    spinner(effectiveProp::value.withType()!!, Int.MIN_VALUE, Int.MAX_VALUE)
                        .applyToComponent {
                            addChangeListener { effectiveProp.preview = value }
                        }
                else -> throw IllegalArgumentException("Not yet implemented!")
            }
            enableIf(effectiveProp.activeCondition)
        }
    }

    private fun Row.addChoiceProperty(choiceProperty: ChoiceProperty<Any, Any>) {
        if (choiceProperty.choices.size >= CHOICE_PROPERTY_GROUPING_THRESHOLD) {
            comboBox(
                CollectionComboBoxModel(choiceProperty.choices),
                choiceProperty::choiceValue,
                renderer = SimpleListCellRenderer.create<Any>("") { choiceProperty.renderer(it) }
            )
        } else {
            buttonGroup {
                choiceProperty.choices.forEach { item ->
                    row {
                        radioButton(choiceProperty.renderer(item)).applyToComponent {
                            isSelected = choiceProperty.value == item
                            addActionListener { if (isSelected) choiceProperty.preview = item }
                        }
                        enableIf(choiceProperty.activeCondition)
                    }
                }
            }
        }
    }

    private fun Row.enableIf(condition: Condition) {
        enableIf(ConditionComponentPredicate(condition))
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
        const val CHOICE_PROPERTY_GROUPING_THRESHOLD = 3
        val UNNAMED_GROUP_TITLE: String? = null
    }
}

internal class DocumentChangeListener(val onChange: () -> Unit) : DocumentListener {

    override fun insertUpdate(e: DocumentEvent?) = onChange()

    override fun removeUpdate(e: DocumentEvent?) = onChange()

    override fun changedUpdate(e: DocumentEvent?) = onChange()
}

internal class ConditionComponentPredicate(private val condition: Condition) : ComponentPredicate() {
    override fun addListener(listener: (Boolean) -> Unit) {
        condition.registerListener(Condition::value) { _, new -> listener(new) }
    }

    override fun invoke() = condition()
}
