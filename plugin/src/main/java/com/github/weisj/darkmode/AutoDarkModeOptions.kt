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

import com.github.weisj.darkmode.platform.ServiceUtil
import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * The storage for plugin options.
 *
 * Settings can be declared by registering a {@link SettingsContainerProvider} service.
 */
@State(name = "AutoDarkMode", storages = [Storage("autoDarkMode.xml", roamingType = RoamingType.PER_OS)])
class AutoDarkModeOptions : PersistentStateComponent<AutoDarkModeOptions.State> {

    companion object {
        private const val ROOT_GROUP_NAME = "__root__group__"
        private const val SETTING_VERSION_NAME = "__settings__version__"
        private const val SETTINGS_VERSION = 1
    }

    /*
     * Because out state immediately gets initialized with default values we need to be careful when
     * to return a complete copy of the state.
     */
    private enum class LoadState {
        // If no state has been requested we return the bare minimum of state data.
        INVALID,

        // After the state has been loaded it will be requested once more to determine the
        // state after loading. Here we simply return the state provided by the loadState method.
        STATE_AFTER_LOAD,

        // After the state has been fully loaded we can return the complete state without it being ignored
        // on save
        LOADED
    }

    private var storageSettingsVersion: Double = SETTINGS_VERSION
    val containers: List<SettingsContainer> by lazy {
        ServiceUtil.load(SettingsContainerProvider::class.java)
            .filter { it.enabled }
            .map { it.create() }
            .onEach { it.init() }
            .asSequence()
            .toList()
    }
    private val properties: MutableMap<PropertyIdentifier, PersistentValueProperty<Any>> by lazy {
        initState(containers, mutableMapOf())
    }
    private var stateAfterLoad: State? = null
    private var loadState = LoadState.INVALID

    private fun initState(
        cont: List<SettingsContainer>,
        props: MutableMap<PropertyIdentifier, PersistentValueProperty<Any>>
    ): MutableMap<PropertyIdentifier, PersistentValueProperty<Any>> {
        cont.flatMap { it.allProperties() }
            .mapNotNull { it.asPersistent() }
            .forEach {
                val identifier = it.propertyIdentifier
                props[identifier]?.let { other ->
                    throw IllegalStateException(
                        "$it clashes with $other. Property with identifier $identifier already defined."
                    )
                }
                props[it.propertyIdentifier] = it
            }
        return props
    }

    private fun createVersionEntry(): Entry = Entry(ROOT_GROUP_NAME, SETTING_VERSION_NAME, SETTINGS_VERSION.toString())

    private fun computeStateEntries(): List<Entry> {
        val props = when (loadState) {
            LoadState.LOADED -> properties.map { (k, v) -> Entry(k.groupIdentifier, k.name, v.value) }
            LoadState.STATE_AFTER_LOAD -> {
                val entries = stateAfterLoad?.entries ?: emptyList()
                stateAfterLoad = null
                entries
            }
            else -> emptyList()
        }
        val versionEntry = createVersionEntry()
        return (props + versionEntry)
    }

    override fun getState(): State {
        val state = State(computeStateEntries())
        if (loadState == LoadState.STATE_AFTER_LOAD) {
            loadState = LoadState.LOADED
        }
        return state
    }

    override fun loadState(toLoad: State) {
        if (loadState == LoadState.INVALID) {
            stateAfterLoad = toLoad
            loadState = LoadState.STATE_AFTER_LOAD
        }
        storageSettingsVersion = toLoad.entries.find {
            it.groupIdentifier == ROOT_GROUP_NAME && it.name == SETTING_VERSION_NAME
        }?.value?.toDouble() ?: SETTINGS_VERSION

        val keepUnused = storageSettingsVersion >= SETTINGS_VERSION
        toLoad.entries.forEach {
            val identifier = PropertyIdentifier(it.groupIdentifier, it.name)
            if (keepUnused) {
                properties.getOrPut(
                    identifier,
                    { PersistentValuePropertyStub(it.name, it.value, it.groupIdentifier) }
                ).value = it.value
            } else {
                properties[identifier]?.value = it.value
            }
        }
        settingsLoaded()
    }

    fun settingsLoaded() {
        containers.forEach { it.onSettingsLoaded() }
    }

    data class PropertyIdentifier(val groupIdentifier: String, val name: String)

    private val <T> ValueProperty<T>.propertyIdentifier
        get() = PropertyIdentifier(group.getIdentifierPath(), name)

    data class State(var entries: List<Entry> = emptyList())

    data class Entry(var groupIdentifier: String = "", var name: String = "", var value: String = "")

    private class PersistentValuePropertyStub(
        override val name: String,
        override var value: String,
        groupIdentifier: String
    ) : PersistentValueProperty<Any>, Observable<ValueProperty<String>> by DefaultObservable() {
        override val group: SettingsGroup = DefaultSettingsGroup(null, groupIdentifier)
        override val backingProperty: ValueProperty<Any>
            get() = throw IllegalStateException("Not supported")
        override var activeCondition = conditionOf(true)
        override var preview: String = value
        override val description: String = ""
    }
}
