package com.github.weisj.darkmode.platform.settings

import kotlin.reflect.KMutableProperty0

private data class Counter(var count : Int) {

    fun increment() : Int {
        val c = count
        count++
        return c
    }
}

private val SettingsContainer.unnamedGroupCounter by lazy { Counter(0) }

fun SettingsContainer.group(name: String = "", init: SettingsGroup.() -> Unit) : SettingsGroup {
    val identifier = if (name.isEmpty()) "${this.identifier}:group_${unnamedGroupCounter.increment()}" else null
    val group = DefaultNamedSettingsGroup(this, name, identifier)
    namedGroups.add(group)
    group.init()
    return group
}

fun SettingsContainer.unnamedGroup(init: SettingsGroup.() -> Unit) : SettingsGroup = this.also(init)

fun SettingsContainer.hidden(init: SettingsGroup.() -> Unit) : SettingsGroup = hiddenGroup.also(init)

fun <T : ValueProperty<T>> SettingsGroup.property(property: T, init: T.() -> Unit = {}): T =
    property.also { it.init(); add(it) }

fun <T : Any> SettingsGroup.property(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<T>,
    init: SimpleValueProperty<T>.() -> Unit = {}
): ValueProperty<T> =
    SimpleValueProperty(name, description, value, this).also { it.init(); add(it) }

fun <R : Any, T : Any> SettingsGroup.property(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>,
    init: SimpleValueProperty<R>.() -> Unit = {}
): TransformingValueProperty<R, T> =
    SimpleTransformingValueProperty(
        SimpleValueProperty(name, description, value, this).also(init),
        transformer
    ).also { add(it) }

fun SettingsGroup.stringProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<String>
): ValueProperty<String> = property(name, description, value)

fun SettingsGroup.booleanProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<Boolean>,
    init: SimpleBooleanProperty.() -> Unit = {}
): ValueProperty<Boolean> =
    SimpleBooleanProperty(SimpleValueProperty(name, description, value, this)).also { it.init(); add(it) }

fun <R : Any, T : Any> SettingsGroup.choiceProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>,
    init: ChoiceProperty<R, T>.() -> Unit = {}
): ChoiceProperty<R, T> =
    TransformingChoiceProperty(SimpleValueProperty(name, description, value, this), transformer).also {
        it.init(); add(
        it
    )
    }

fun <T : Any> SettingsGroup.choiceProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<T>,
    init: ChoiceProperty<T, T>.() -> Unit = {}
): ChoiceProperty<T, T> =
    TransformingChoiceProperty<T, T>(
        SimpleValueProperty(name, description, value, this),
        identityTransformer()
    ).also { it.init(); add(it) }

fun <R : Any> SettingsGroup.persistentProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, String>,
    init: SimpleValueProperty<R>.() -> Unit = {}
): PersistentValueProperty<R> =
    SimplePersistentValueProperty(
        SimpleValueProperty(name, description, value, this).also(init),
        transformer
    ).also { add(it) }

fun SettingsGroup.persistentStringProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<String>
): ValueProperty<String> = persistentProperty(name, description, value, identityTransformer())

fun SettingsGroup.persistentBooleanProperty(
    description: String? = null,
    value: KMutableProperty0<Boolean>,
    name: String? = null,
    init: SimplePersistentBooleanProperty.() -> Unit = {}
): PersistentValueProperty<Boolean> =
    SimplePersistentBooleanProperty(
        SimplePersistentValueProperty(
            SimpleValueProperty(name, description, value, this),
            transformerOf(String::toBoolean, Boolean::toString)
        )
    ).also { it.init(); add(it) }

fun <R : Any> SettingsGroup.persistentChoiceProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, String>,
    init: ChoiceProperty<R, String>.() -> Unit = {}
): ChoiceProperty<R, String> =
    PersistentChoiceProperty(
        SimpleValueProperty(name, description, value, this),
        transformer
    ).also { it.init(); add(it) }

fun SettingsGroup.persistentChoiceProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<String>,
    init: ChoiceProperty<String, String>.() -> Unit = {}
): ChoiceProperty<String, String> = choiceProperty(name, description, value, init)

