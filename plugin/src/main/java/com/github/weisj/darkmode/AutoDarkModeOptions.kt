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
@State(name = "AutoDarkMode2", storages = [Storage("auto-dark-mode2.xml", roamingType = RoamingType.PER_OS)])
class AutoDarkModeOptions : PersistentStateComponent<AutoDarkModeOptions.State> {

    val properties: MutableMap<String, ValueProperty<Any>> = HashMap()
    val containers: List<SettingsContainer> = ServiceUtil.load(SettingsContainer::class.java).asSequence().toList()

    init {
        containers.flatMap { it.allProperties() }.forEach { properties[it.property.name] = it }
    }

    inline fun <reified T : Any> getProperty(name: String): KMutableProperty0<T>? {
        return properties[name]?.property?.withType()
    }

    inline fun <reified T : Any> getReadProperty(name: String): KMutableProperty0<T>? {
        return properties[name]?.property?.withOutType()
    }

    inline fun <reified T : Any> getWriteProperty(name: String): KMutableProperty0<T>? {
        return properties[name]?.property?.withInType()
    }

    inline fun <reified T : Any> readOrNull(name: String): T? = getReadProperty<T>(name)?.get()

    inline fun <reified T : Any> read(name: String): T = readOrNull(name)!!

    inline fun <reified T : Any> write(name: String, value: T) = getWriteProperty<T>(name)?.set(value)

    override fun getState(): State? {
        return State(
            properties
                .mapValues { e -> e.value.property.get().let { e.value.writer(it) } }
                .map { (k, v) -> Entry(k, v) }
                .toTypedArray()
        )
    }

    override fun loadState(toLoad: State) {
        toLoad.entries?.forEach { p ->
            properties[p.key]?.let { it.property.set(it.parser(p.value)) }
        }
    }

    data class State(var entries: Array<Entry>? = arrayOf()) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as State

            if (entries != null) {
                if (other.entries == null) return false
                if (!entries!!.contentEquals(other.entries!!)) return false
            } else if (other.entries != null) return false

            return true
        }

        override fun hashCode(): Int {
            return entries?.contentHashCode() ?: 0
        }
    }

    data class Entry(var key: String = "", var value: String = "")
}
