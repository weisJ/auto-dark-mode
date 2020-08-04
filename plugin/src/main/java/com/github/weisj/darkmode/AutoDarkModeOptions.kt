package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.ServiceUtil
import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlin.reflect.KMutableProperty0

/**
 * The storage for plugin options.
 *
 * Settings can be declared by registering a {@link SettingsContainerProvider} service.
 */
@State(name = "AutoDarkMode", storages = [Storage("autoDarkMode.xml", roamingType = RoamingType.PER_OS)])
class AutoDarkModeOptions : PersistentStateComponent<AutoDarkModeOptions.State> {

    private val properties: MutableMap<PropertyIdentifier, PersistentValueProperty<Any>> = HashMap()
    val containers: List<SettingsContainer> =
        ServiceUtil.load(SettingsContainerProvider::class.java)
            .filter { it.enabled }
            .map { it.create() }
            .asSequence()
            .toList()

    init {
        containers
            .flatMap { it.allProperties() }
            .mapNotNull { it.asPersistent() }
            .forEach {
                val identifier = it.identifier
                properties[identifier]?.let { other ->
                    throw IllegalStateException(
                        "$it clashes with $other. Property with identifier $identifier already defined."
                    )
                }
                properties[it.identifier] = it
            }
    }

    override fun getState(): State? {
        return State(properties.map { (k, v) -> Entry(k.groupIdentifier, k.name, v.value) }.toMutableList())
    }

    override fun loadState(toLoad: State) {
        toLoad.entries.forEach {
            properties.getOrPut(
                PropertyIdentifier(it.groupIdentifier, it.name),
                { PersistentValuePropertyStub(it.name, it.value, it.groupIdentifier) }
            ).value = it.value
        }
        settingsLoaded()
    }

    fun settingsLoaded() {
        containers.forEach { it.onSettingsLoaded() }
    }

    data class PropertyIdentifier(val groupIdentifier: String, val name: String)

    private val <T> ValueProperty<T>.identifier
        get() = PropertyIdentifier(group.identifier, name)

    data class State(var entries: MutableList<Entry> = mutableListOf())

    data class Entry(var groupIdentifier: String = "", var name: String = "", var value: String = "")

    private class PersistentValuePropertyStub(
        override val name: String,
        override var value: String,
        groupIdentifier: String,
        override val description: String = "",
        override var active: Boolean = true
    ) : PersistentValueProperty<Any>, Observable<ValueProperty<*>> by DefaultObservable() {
        override val group: SettingsGroup = DefaultSettingsGroup(null, groupIdentifier)
        override var backingValue: Any by ::value.withOutType()
    }
}
