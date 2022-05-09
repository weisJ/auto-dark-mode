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

import com.github.weisj.darkmode.platform.settings.ChoiceProperty
import com.github.weisj.darkmode.platform.settings.Condition
import com.github.weisj.darkmode.platform.settings.NamedSettingsGroup
import com.github.weisj.darkmode.platform.settings.SettingsGroup
import com.github.weisj.darkmode.platform.settings.ValueProperty
import com.github.weisj.darkmode.platform.settings.effective
import com.github.weisj.darkmode.platform.settings.isTotallyEmpty
import com.github.weisj.darkmode.platform.settings.registerListener
import com.github.weisj.darkmode.platform.settings.withType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.castSafelyTo
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent

class DarkModeConfigurable : BoundConfigurable(SETTINGS_TITLE) {

    override fun createPanel(): DialogPanel {
        val options = ApplicationManager.getApplication().getService(AutoDarkModeOptions::class.java)

        return panel {
            options.containers.forEach { container ->
                container.subgroups.forEach { addGroup(it) }
                addGroup(container.unnamedGroup, UNNAMED_GROUP_TITLE)
            }
        }
    }

    private fun Panel.addGroup(group: NamedSettingsGroup) = addGroup(group, group.name)

    private fun Panel.addGroup(
        properties: SettingsGroup,
        name: String?
    ) {
        if (properties.isTotallyEmpty()) return
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

    private fun Panel.addProperty(valueProp: ValueProperty<Any>) {
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
                    checkBox(valueProp.description)
                        .bindSelected(effectiveProp::value.withType()!!)
                        .applyToComponent {
                            addActionListener { effectiveProp.preview = isSelected }
                        }
                prop is String ->
                    textField()
                        .bindText(effectiveProp::value.withType()!!)
                        .applyToComponent {
                            document.addDocumentListener(
                                DocumentChangeListener {
                                    effectiveProp.preview = text
                                }
                            )
                        }
                prop is Int ->
                    spinner(Int.MIN_VALUE .. Int.MAX_VALUE)
                        .bindIntValue(effectiveProp::value.withType()!!)
                        .applyToComponent {
                            addChangeListener { effectiveProp.preview = value }
                        }
                else -> throw IllegalArgumentException("Not yet implemented!")
            }
            enableIf(effectiveProp.activeCondition)
        }
    }

    private fun Row.addChoiceProperty(choiceProperty: ChoiceProperty<Any, Any>) {
        val choiceModel = CollectionComboBoxModel(choiceProperty.choicesProvider().toMutableList())
        comboBox(
            choiceModel,
            renderer = SimpleListCellRenderer.create("<null>") { choiceProperty.renderer(it) }
        ).bindItem({ choiceProperty.choiceValue }, { if (it != null) choiceProperty.choiceValue = it })
            .applyToComponent {
            addPopupMenuListener(object : PopupMenuListenerAdapter() {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                    val selected = choiceModel.selected
                    choiceModel.removeAll()
                    choiceModel.addAll(0, choiceProperty.choicesProvider())
                    choiceModel.selectedItem =
                        selected ?: if (choiceModel.isEmpty) null else choiceModel.getElementAt(0)
                }
            })
        }
    }

    private fun Row.enableIf(condition: Condition) {
        enabledIf(ConditionComponentPredicate(condition))
    }

    private fun Panel.maybeTitledRow(name: String?, init: Panel.() -> Unit): RowsRange {
        return if (!name.isNullOrEmpty()) groupRowsRange(name, init = init) else groupRowsRange { init() }
    }

    private fun Panel.maybeNamedRow(name: String?, init: Row.() -> Unit) {
        if (!name.isNullOrEmpty()) row(name, init = init) else row { init() }
    }

    override fun apply() {
        super.apply()
        ApplicationManager.getApplication().getService(AutoDarkMode::class.java).onSettingsChange()
    }

    companion object {
        const val SETTINGS_TITLE: String = "Auto Dark Mode"
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
