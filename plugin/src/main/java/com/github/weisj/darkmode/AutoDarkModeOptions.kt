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
 * Settings can be declared by registering a {@link SettingsContainer} service.
 *
 * Options can be read and written to by addressing them with their name.
 * Though accessing fields directly through this class is discouraged. Instead use them through the
 * {@link SettingsContainer} that provides them.
 */
@State(name = "AutoDarkMode", storages = [Storage("autoDarkMode.xml", roamingType = RoamingType.PER_OS)])
class AutoDarkModeOptions : PersistentStateComponent<AutoDarkModeOptions.State> {

    val properties: MutableMap<PropertyIdentifier, PersistentValueProperty<Any>> = HashMap()
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

    inline fun <reified T : Any> getProperty(identifier: PropertyIdentifier): KMutableProperty0<T>? {
        return properties[identifier]?.let { it::backingValue.withType() }
    }

    inline fun <reified T : Any> getReadProperty(identifier: PropertyIdentifier): KMutableProperty0<T>? {
        return properties[identifier]?.let { it::backingValue.withOutType() }
    }

    inline fun <reified T : Any> getWriteProperty(identifier: PropertyIdentifier): KMutableProperty0<T>? {
        return properties[identifier]?.let { it::backingValue.withInType() }
    }

    inline fun <reified T : Any> readOrNull(identifier: PropertyIdentifier): T? = getReadProperty<T>(identifier)?.get()

    inline fun <reified T : Any> read(identifier: PropertyIdentifier): T = readOrNull(identifier)!!

    inline fun <reified T : Any> write(identifier: PropertyIdentifier, value: T) =
        getWriteProperty<T>(identifier)?.set(value)

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
