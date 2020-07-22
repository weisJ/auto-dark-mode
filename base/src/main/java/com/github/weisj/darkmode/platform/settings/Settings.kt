package com.github.weisj.darkmode.platform.settings

import com.intellij.util.castSafelyTo
import kotlin.reflect.KMutableProperty0

interface SettingsContainer : SettingsGroup {
    val namedGroups: MutableList<NamedSettingsGroup>
    val unnamedGroup: SettingsGroup
    val enabled : Boolean

    @JvmDefault
    fun allProperties(): List<ValueProperty<Any>> {
        return namedGroups.flatten() + unnamedGroup
    }
}

/**
 * Container for {@link ValueProperty}s. Properties can be group into
 * logical units using a {@SettingsGroup}.
 *
 * All properties not contained inside a {@SettingsGroup} will automatically belong
 * to the unnamed group of the container.
 */
abstract class DefaultSettingsContainer private constructor(
    override val unnamedGroup: SettingsGroup,
    override val enabled: Boolean
) : SettingsContainer, SettingsGroup by unnamedGroup {

    constructor(enabled : Boolean = true) : this(DefaultSettingsGroup(), enabled)

    override val namedGroups: MutableList<NamedSettingsGroup> = mutableListOf()
}

fun <T> SettingsGroup.add(property: ValueProperty<T>) {
    this.add(property.castSafelyTo()!!)
}

/**
 * Provides grouping for {@link ValueProperty}s
 */
typealias SettingsGroup = MutableList<ValueProperty<Any>>

interface NamedSettingsGroup : SettingsGroup {
    val name: String
}

open class DefaultSettingsGroup internal constructor(
    private val properties: MutableList<ValueProperty<Any>> = mutableListOf()
) : SettingsGroup by properties

class DefaultNamedSettingsGroup internal constructor(
    override val name: String
) : DefaultSettingsGroup(), NamedSettingsGroup

/**
 * Wrapper for properties that provides a description and parser/writer used
 * for persistent storage.
 */
interface ValueProperty<T> {
    val description: String
    val name: String
    var value: T
}

/**
 * Property with a backing value that has a different type than the exposed value.
 */
interface TransformingValueProperty<R, T> : ValueProperty<T> {
    var backingValue: R
}

/**
 * Property that can be stored in String format.
 */
interface PersistentValueProperty<T> : TransformingValueProperty<T, String>

fun ValueProperty<*>.toTransformer(): TransformingValueProperty<Any, Any>? =
    castSafelyTo<TransformingValueProperty<Any, Any>>()

inline fun <reified T: Any> ValueProperty<T>.asPersistent() : PersistentValueProperty<T>?
        = castSafelyTo<PersistentValueProperty<T>>()

/**
 * The effective value of the property. If the property is a transforming property the
 * backing field is chosen. Because of this for a reference to a simple ValueProperty<T>
 * the most general value that can be returned is Any.
 */
val <T : Any> ValueProperty<T>.effectiveProperty : KMutableProperty0<Any>
    get() = toTransformer()?.let { it::backingValue } ?: this::value.withOutType()!!

// Offers type specific overload of effective property for transforming properties.
val <R, T> TransformingValueProperty<R, T>.effective: KMutableProperty0<R>
    get() = ::backingValue


class SimpleValueProperty<T> internal constructor(
    descr: String?,
    private val property: KMutableProperty0<T>
) : ValueProperty<T> {
    override val description: String = descr ?: property.name
    override val name: String = property.name
    override var value: T
        get() = property.get()
        set(v) = property.set(v)
}

open class SimpleTransformingValueProperty<R, T> internal constructor(
    private val delegate: ValueProperty<R>,
    private var transformer: Transformer<R, T>
) : TransformingValueProperty<R, T> {
    override val description = delegate.description
    override val name = delegate.name
    override var value: T
        get() = transformer.read(delegate.value)
        set(v) {
            delegate.value = transformer.write(v)
        }
    override var backingValue: R
        get() = delegate.value
        set(v) {
            delegate.value = v
        }
}

class SimplePersistentValueProperty<R>(
    delegate: ValueProperty<R>,
    transformer: Transformer<R, String>
) : SimpleTransformingValueProperty<R, String>(delegate, transformer),
    PersistentValueProperty<R>

/**
 * Property that has a limited set of values the property can take on.
 */
abstract class ChoiceProperty<R, T> internal constructor(
    private val delegateProperty: TransformingValueProperty<R, T>
) : TransformingValueProperty<R, T> by delegateProperty {
    var choiceValue: R
        get() = backingValue
        set(v) {
            backingValue = v
        }
    var choices: List<R> = ArrayList()
    var renderer: (R) -> String = { it.toString() }
}

class TransformingChoiceProperty<R, T> internal constructor(
    property: TransformingValueProperty<R, T>
) : ChoiceProperty<R, T>(property) {
    constructor(property: ValueProperty<R>, transformer: Transformer<R, T>)
            : this(SimpleTransformingValueProperty(property, transformer))
}

class PersistentChoiceProperty<R>(
    property: PersistentValueProperty<R>
) : ChoiceProperty<R, String>(property),
    PersistentValueProperty<R> {
    constructor(property: ValueProperty<R>, transformer: Transformer<R, String>)
            : this(SimplePersistentValueProperty(property, transformer))
}


fun SettingsContainer.group(name: String, init: SettingsGroup.() -> Unit) {
    val group = DefaultNamedSettingsGroup(name)
    namedGroups.add(group)
    group.init()
}

fun SettingsContainer.unnamedGroup(init: SettingsGroup.() -> Unit) = this.init()

fun <T> SettingsGroup.property(
    description: String? = null,
    value: KMutableProperty0<T>
) = add(SimpleValueProperty(description, value))

fun <R, T> SettingsGroup.property(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>
) = add(SimpleTransformingValueProperty(SimpleValueProperty(description, value), transformer))

fun SettingsGroup.stringProperty(description: String? = null, value: KMutableProperty0<String>) =
    property(description, value)

fun SettingsGroup.booleanProperty(description: String? = null, value: KMutableProperty0<Boolean>) =
    property(description, value)

fun <R, T> SettingsGroup.choiceProperty(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>,
    init: ChoiceProperty<R, T>.() -> Unit = {}
) = add(TransformingChoiceProperty(SimpleValueProperty(description, value), transformer).also(init))

fun <R> SettingsGroup.persistentProperty(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, String>
) = add(SimplePersistentValueProperty(SimpleValueProperty(description, value), transformer))

fun SettingsGroup.persistentStringProperty(description: String? = null, value: KMutableProperty0<String>) =
    persistentProperty(description, value, identityTransformer())

fun SettingsGroup.persistentBooleanProperty(description: String? = null, value: KMutableProperty0<Boolean>) =
    persistentProperty(description, value, transformerOf(String::toBoolean, Boolean::toString))

fun <R> SettingsGroup.persistentChoiceProperty(
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, String>,
    init: ChoiceProperty<R, String>.() -> Unit = {}
) = add(PersistentChoiceProperty(SimpleValueProperty(description, value), transformer).also(init))

