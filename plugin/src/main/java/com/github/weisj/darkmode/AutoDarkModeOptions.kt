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

    private val properties: MutableMap<PropertyIdentifier, PersistentValueProperty<Any>> = HashMap()
    val containers: List<SettingsContainer> =
        ServiceUtil.load(SettingsContainerProvider::class.java)
            .filter { it.enabled }
            .map { it.create() }
            .onEach { it.init() }
            .asSequence()
            .toList()

    init {
        containers
            .flatMap { it.allProperties() }
            .mapNotNull { it.asPersistent() }
            .forEach {
                val identifier = it.propertyIdentifier
                properties[identifier]?.let { other ->
                    throw IllegalStateException(
                        "$it clashes with $other. Property with identifier $identifier already defined."
                    )
                }
                properties[it.propertyIdentifier] = it
            }
    }

    override fun getState(): State? {
        return State(properties.map { (k, v) -> Entry(k.groupIdentifier, k.name, v.value) }.toMutableList().also {
            it.add(Entry(ROOT_GROUP_NAME, SETTING_VERSION_NAME, SETTINGS_VERSION.toString()))
        })
    }

    override fun loadState(toLoad: State) {
        val keepUnused =
            toLoad.entries.find { it.groupIdentifier == ROOT_GROUP_NAME && it.name == SETTING_VERSION_NAME }?.let {
                it.value.toInt() >= SETTINGS_VERSION
            } ?: false
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

    data class State(var entries: MutableList<Entry> = mutableListOf())

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
