package com.github.weisj.darkmode

import com.github.weisj.darkmode.platform.ServiceUtil
import com.github.weisj.darkmode.platform.settings.*
import com.intellij.openapi.components.*
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

    val properties: MutableMap<String, PersistentValueProperty<Any>> = HashMap()
    val containers: List<SettingsContainer> =
        ServiceUtil.load(SettingsContainer::class.java).filter(SettingsContainer::isEnabled).asSequence().toList()

    init {
        containers
            .flatMap { it.allProperties() }
            .mapNotNull { it.asPersistent() }
            .forEach { properties[it.name] = it }
    }

    inline fun <reified T : Any> getProperty(name: String): KMutableProperty0<T>? {
        return properties[name]?.let { it::backingValue.withType() }
    }

    inline fun <reified T : Any> getReadProperty(name: String): KMutableProperty0<T>? {
        return properties[name]?.let { it::backingValue.withOutType() }
    }

    inline fun <reified T : Any> getWriteProperty(name: String): KMutableProperty0<T>? {
        return properties[name]?.let { it::backingValue.withInType() }
    }

    inline fun <reified T : Any> readOrNull(name: String): T? = getReadProperty<T>(name)?.get()

    inline fun <reified T : Any> read(name: String): T = readOrNull(name)!!

    inline fun <reified T : Any> write(name: String, value: T) = getWriteProperty<T>(name)?.set(value)

    override fun getState(): State? {
        return State(properties.map { (k, v) -> Entry(k, v.value) }.toMutableList())
    }

    override fun loadState(toLoad: State) {
        toLoad.entries.forEach { properties[it.key]?.value = it.value }
    }

    data class State(var entries: MutableList<Entry> = mutableListOf())

    data class Entry(var key : String = "", var value : String = "")
}
