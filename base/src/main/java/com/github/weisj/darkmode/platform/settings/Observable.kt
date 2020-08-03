package com.github.weisj.darkmode.platform.settings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*

typealias BiConsumer<T> = (T, T) -> Unit

class ObservableManager<T> {
    private val properties = mutableMapOf<String, Any>()
    private val listeners = mutableMapOf<String, MutableList<BiConsumer<Any>>>()

    private fun updateValue(name: String, value: Any) {
        val old = properties.put(name, value)!!
        listeners[name]?.forEach { it(old, value) }
    }

    fun registerListener(name: String, biConsumer: BiConsumer<Any>) {
        listeners.getOrPut(name) { mutableListOf() }.add(biConsumer)
        properties[name]?.let { biConsumer(it, it) }
    }

    inline fun <reified V : Any> registerListener(property: KProperty1<T, V>, crossinline consumer: BiConsumer<V>) =
        registerListener(property.name) { old, new -> consumer(old as V, new as V) }

    inner class WrappingRWProperty<V : Any>(
        prop: KProperty<*>,
        value: V,
        private val type: KClass<V>
    ) : ReadWriteProperty<T, V> {

        init {
            properties[prop.name] = value
        }

        @ExperimentalStdlibApi
        override operator fun getValue(thisRef: T, property: KProperty<*>) =
            type.safeCast(properties[property.name])!!

        override operator fun setValue(thisRef: T, property: KProperty<*>, value: V) =
            updateValue(property.name, value)
    }
}

interface DelegateProvider<T, V> {
    operator fun provideDelegate(
        thisRef: T,
        prop: KProperty<*>
    ): ReadWriteProperty<T, V>
}

class ObservableValue<T, V : Any>(
    private val delegate: ObservableManager<T>,
    private val value: V,
    private val type: KClass<V>
) : DelegateProvider<T, V> {
    override operator fun provideDelegate(
        thisRef: T,
        prop: KProperty<*>
    ): ReadWriteProperty<T, V> = delegate.WrappingRWProperty(prop, value, type)
}

interface Observable<T> {
    val manager: ObservableManager<T>
}

open class DefaultObservable<T> : Observable<T> {
    override val manager = ObservableManager<T>()
}

inline fun <T, reified V : Any> Observable<T>.observable(value: V): DelegateProvider<T, V> =
    ObservableValue(manager, value, V::class)

inline fun <T, reified V : Any> Observable<T>.registerListener(
    property: KProperty1<T, V>,
    crossinline consumer: BiConsumer<V>
) = manager.registerListener(property, consumer)
