package com.github.weisj.darkmode.platform.settings

import com.intellij.util.castSafelyTo
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0

/**
 * Cast the property to the specified type if applicable for read and write operations.
 */
inline fun <reified T> KMutableProperty0<Any>.withType(): KMutableProperty0<T>? {
    return if (get().javaClass.kotlin == T::class) {
        this.castSafelyTo<KMutableProperty0<T>>()
    } else {
        null
    }
}

/**
 * Cast the property to the specified type if applicable for read operations.
 */
inline fun <reified T> KMutableProperty0<Any>.withOutType(): KMutableProperty0<T>? {
    return if (T::class.java.isAssignableFrom(get().javaClass)) {
        this.castSafelyTo<KMutableProperty0<T>>()
    } else {
        null
    }
}

/**
 * Cast the property to the specified type if applicable for write operations.
 */
inline fun <reified T> KMutableProperty0<Any>.withInType(): KMutableProperty0<T>? {
    return if (get().javaClass.isAssignableFrom(T::class.java)) {
        this.castSafelyTo<KMutableProperty0<T>>()
    } else {
        null
    }
}

interface SettingsContainer : SettingsGroup {
    val settings: MutableMap<KClass<*>, MutableMap<String, ValueProperty<Any>>>
    val namedGroups: MutableList<SettingsGroup>
    val unnamedGroup: SettingsGroup

    @JvmDefault
    fun allProperties(): List<ValueProperty<Any>> {
        return settings.values.flatMap { it.values }
    }
}

/**
 * Container for {@link ValueProperty}s. Properties can be group into
 * logical units using a {@SettingsGroup}.
 *
 * All properties not contained inside a {@SettingsGroup} will automatically belong
 * to the unnamed group of the container.
 */
open class DefaultSettingsContainer(
    override val unnamedGroup: SettingsGroup = DefaultSettingsGroup()
) : SettingsContainer, SettingsGroup by unnamedGroup {
    override val settings: MutableMap<KClass<*>, MutableMap<String, ValueProperty<Any>>> = mutableMapOf()
    override val namedGroups: MutableList<SettingsGroup> = mutableListOf()

    init {
        unnamedGroup.parent = this
    }
}

inline fun <reified T> SettingsGroup.add(property: ValueProperty<T>) {
    val type = T::class
    val list = parent.settings[type] ?: mutableMapOf()
    list[property.description] = property.castSafelyTo()!!
    parent.settings[type] = list
    properties.add(property.castSafelyTo()!!)
}

/**
 * Provides grouping for {@link ValueProperty}s
 */
interface SettingsGroup {
    val name: String
    val properties: MutableList<ValueProperty<Any>>
    var parent: SettingsContainer
}

class DefaultSettingsGroup(
    override val name: String = "",
    override val properties: MutableList<ValueProperty<Any>> = mutableListOf()
) : SettingsGroup {
    override lateinit var parent: SettingsContainer
}

/**
 * Wrapper for properties that provides a description and parser/writer used
 * for persistent storage.
 */
interface ValueProperty<T> {
    val description: String
    var property: KMutableProperty0<T>
    val parser: (String) -> T
    val writer: (T) -> String
}

class SimpleValueProperty<T>(
    override val description: String,
    override var property: KMutableProperty0<T>,
    override val parser: (s: String) -> T,
    override val writer: (T) -> String = { it.toString() }
) : ValueProperty<T>

/**
 * Property that has a limited set of values the property can take on.
 */
class ChoiceProperty<T>(
    override val description: String,
    override var property: KMutableProperty0<T>,
    override val parser: (s: String) -> T,
    override val writer: (T) -> String = { it.toString() }
) : ValueProperty<T> {
    var options: List<T> = ArrayList()
    var renderer: (T) -> String = { it.toString() }
}

fun SettingsContainer.group(name: String, init: SettingsGroup.() -> Unit) {
    val group = DefaultSettingsGroup(name)
    group.parent = this
    parent.namedGroups.add(group)
    group.init()
}

inline fun <reified T> SettingsGroup.property(
    description: String,
    value: KMutableProperty0<T>,
    noinline parser: (s: String) -> T
) = add(SimpleValueProperty(description, value, parser))

inline fun <reified T> SettingsGroup.property(
    description: String,
    value: KMutableProperty0<T>,
    noinline parser: (String) -> T,
    noinline writer: (T) -> String
) = add(SimpleValueProperty(description, value, parser, writer))

inline fun <reified T> SettingsGroup.choiceProperty(
    description: String,
    value: KMutableProperty0<T>,
    noinline parser: (s: String) -> T,
    init: ChoiceProperty<T>.() -> Unit
) = add(ChoiceProperty(description, value, parser).also { it.init() })

inline fun <reified T> SettingsGroup.choiceProperty(
    description: String,
    value: KMutableProperty0<T>,
    noinline parser: (String) -> T,
    noinline writer: (T) -> String,
    init: ChoiceProperty<T>.() -> Unit
) = add(ChoiceProperty(description, value, parser, writer).also { it.init() })

inline fun <reified T> SettingsGroup.choiceProperty(
    value: KMutableProperty0<T>,
    noinline parser: (String) -> T,
    noinline writer: (T) -> String,
    init: ChoiceProperty<T>.() -> Unit
) = choiceProperty(value.name, value, parser, writer, init)

inline fun <reified T> SettingsGroup.property(value: KMutableProperty0<T>, noinline parser: (s: String) -> T) =
    property(value.name, value, parser)

inline fun <reified T> SettingsGroup.property(
    value: KMutableProperty0<T>,
    noinline parser: (String) -> T,
    noinline writer: (T) -> String
) = property(value.name, value, parser, writer)

fun SettingsGroup.stringProperty(description: String, value: KMutableProperty0<String>) =
    property(description, value) { it }

fun SettingsGroup.booleanProperty(description: String, value: KMutableProperty0<Boolean>) =
    property(description, value, String::toBoolean)

fun SettingsGroup.booleanProperty(value: KMutableProperty0<Boolean>) =
    booleanProperty(value.name, value)
