package com.github.weisj.darkmode.platform.settings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty


/**
 * Transformer for changing the outwards type of a property.
 * R is the real type and T the transformed type.
 */
interface Transformer<R, T> {
    val write: (T) -> R
    val read: (R) -> T
}

class DefaultTransformer<R, T>(
    override val write: (T) -> R,
    override val read: (R) -> T
) : Transformer<R, T>

infix fun <R, T, S> Transformer<R, T>.andThen(other: Transformer<T, S>): Transformer<R, S> {
    return transformerOf(other.write andThen write, read andThen other.read)
}

fun <R, T> Transformer<R, T?>.readFallback(fallback: T): Transformer<R, T> {
    return this andThen transformerOf({ t -> t }, { t -> t ?: fallback })
}

fun <R, T> Transformer<R?, T>.writeFallback(fallback: R): Transformer<R, T> {
    return transformerOf<R, R?>({ t -> t ?: fallback }, { t -> t }) andThen this
}

object IdentityTransformer : Transformer<Any, Any> by DefaultTransformer({ t -> t }, { t -> t })

fun <T : Any> identityTransformer(): Transformer<T, T> =
    IdentityTransformer.castSafelyTo<Transformer<T, T>>()!!

fun <R, T> transformerOf(write: (T) -> R, read: (R) -> T) = DefaultTransformer(write, read)

internal class TransformerDelegate<R, T>(
    private val prop : KMutableProperty0<R>,
    private val transformer : Transformer<R, T>
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return transformer.read(prop.get())
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        prop.set(transformer.write(value))
    }
}

fun <R, T> Transformer<R, T>.delegate(backingProp : KMutableProperty0<R>) : ReadWriteProperty<Any, T> =
    TransformerDelegate(backingProp, this)
