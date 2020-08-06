package com.github.weisj.darkmode.platform.settings

import kotlin.reflect.KProperty0

interface Condition : () -> Boolean, Observable<Condition> {
    var value: Boolean

    fun build() {
        this()
    }
}

class ConstantCondition(value: Boolean) : Condition, Observable<Condition> by DefaultObservable() {
    override var value: Boolean = value
        set(_) = throw UnsupportedOperationException()

    override fun invoke(): Boolean {
        return value
    }
}

class DefaultCondition(initial: Boolean, private val cond: () -> Boolean = { initial }) : Condition,
    Observable<Condition> by DefaultObservable() {
    override var value by observable(initial)

    override operator fun invoke(): Boolean {
        value = cond()
        return value
    }
}

class LazyCondition<T : Any>(private val valueProp: Lazy<ValueProperty<T>>, private val expected: T) : Condition,
    Observable<Condition> by DefaultObservable() {
    override var value by observable(true)

    override fun invoke(): Boolean {
        value = valueProp.value.effective(expected::class).preview == expected
        return value
    }

    override fun build() {
        valueProp.value.effective<Boolean>().let {
            it.registerListener(ValueProperty<Boolean>::preview) { _, _ -> this() }
        }
    }
}

class CompoundCondition(
    private val first: Condition,
    private val second: Condition,
    private val combinator: (Boolean, Boolean) -> Boolean
) : Condition, Observable<Condition> by first {
    override var value by observable(combinator(first.value, second.value))

    override fun invoke(): Boolean {
        value = combinator(first(), second())
        return value
    }

    override fun build() {
        first.build()
        second.build()
        first.registerListener(Condition::value) { _, _ ->
            value = combinator(first.value, second())
        }
        second.registerListener(Condition::value) { _, _ ->
            value = combinator(first(), second.value)
        }
    }
}

infix fun Condition.and(other: Condition): Condition = CompoundCondition(this, other, Boolean::and)

infix fun Condition.or(other: Condition): Condition = CompoundCondition(this, other, Boolean::or)

fun not(cond: Condition): Condition = CompoundCondition(cond, conditionOf(true)) { a, _ -> !a }

fun conditionOf(bool: Boolean): Condition = ConstantCondition(bool)

fun conditionOf(cond: () -> Boolean): Condition = DefaultCondition(cond(), cond)

fun conditionOf(prop: KProperty0<Boolean>): Condition = DefaultCondition(prop.get()) { prop.get() }

fun conditionOf(valueProp: ValueProperty<Boolean>): Condition = conditionOf(valueProp::value)

fun conditionOf(valueProp: Lazy<ValueProperty<Boolean>>): Condition = DefaultCondition(true) { valueProp.value.value }

fun <T : Any> isEqual(valueProp: Lazy<ValueProperty<T>>, expected: T) = LazyCondition(valueProp, expected)

fun isTrue(valueProp: Lazy<ValueProperty<Boolean>>): Condition = isEqual(valueProp, true)

fun isFalse(valueProp: Lazy<ValueProperty<Boolean>>): Condition = isEqual(valueProp, false)
