package com.github.weisj.darkmode.platform.settings

import kotlin.reflect.KMutableProperty0

private data class UnnamedGroupCounter(var count: Int) {

    companion object {
        private val counterMap = mutableMapOf<SettingsGroup, UnnamedGroupCounter>()

        fun get(group: SettingsGroup): UnnamedGroupCounter = counterMap.getOrPut(group) { UnnamedGroupCounter(0) }
    }

    fun increment(): Int {
        return count++
    }
}

private val SettingsGroup.unnamedGroupCounter
    get() = UnnamedGroupCounter.get(this)

class SettingsGroupBuilder(group: SettingsGroup) : SettingsGroup by group {
    internal var activeCondition: Condition? = null

    fun KMutableProperty0<Boolean>.isTrue() = isTrue(getWithProperty(this))
    fun KMutableProperty0<Boolean>.isFalse() = isFalse(getWithProperty(this))
    fun <T : Any> KMutableProperty0<T>.isEqual(expected: T) = isEqual(getWithProperty(this), expected)
}

fun SettingsGroupBuilder.activeIf(condition: Condition) {
    activeCondition = condition
}

//fun SettingsGroupBuilder.isTrue(prop: KMutableProperty0<Boolean>) = isTrue(getWithProperty(prop))

private fun initGroup(group: SettingsGroup, init: SettingsGroupBuilder.() -> Unit): SettingsGroup {
    val builder = SettingsGroupBuilder(group)
    builder.init()
    builder.activeCondition?.let { cond ->
        group.forEach {
            if (it.activeCondition is ConstantCondition) {
                if (it.activeCondition.value) {
                    it.activeIf(cond)
                }
            } else {
                it.activeIf(it.activeCondition and cond)
            }
        }
    }
    return group
}

fun SettingsGroup.group(name: String = "", init: SettingsGroupBuilder.() -> Unit): SettingsGroup {
    val identifier = if (name.isEmpty()) "group_${unnamedGroupCounter.increment()}" else null
    val group = DefaultNamedSettingsGroup(this, name, identifier)
    subgroups.add(group)
    return initGroup(group, init)
}

fun SettingsContainer.unnamedGroup(init: SettingsGroup.() -> Unit): SettingsGroup = initGroup(this, init)

fun SettingsContainer.hidden(init: SettingsGroup.() -> Unit): SettingsGroup = initGroup(hiddenGroup, init)

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
    init: ValueProperty<Boolean>.() -> Unit = {}
): ValueProperty<Boolean> = property(name, description, value, init)

fun <R : Any, T : Any> SettingsGroup.choiceProperty(
    name: String? = null,
    description: String? = null,
    value: KMutableProperty0<R>,
    transformer: Transformer<R, T>,
    init: ChoiceProperty<R, T>.() -> Unit = {}
): ChoiceProperty<R, T> =
    TransformingChoiceProperty(SimpleValueProperty(name, description, value, this), transformer).also {
        it.init(); add(it)
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
    init: PersistentValueProperty<Boolean>.() -> Unit = {}
): PersistentValueProperty<Boolean> =
    SimplePersistentValueProperty(
        SimpleValueProperty(name, description, value, this),
        transformerOf(String::toBoolean, Boolean::toString)
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

