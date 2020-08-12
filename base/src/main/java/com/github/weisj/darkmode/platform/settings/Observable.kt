/*
 * MIT License
 *
 * Copyright (c) 2020 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.github.weisj.darkmode.platform.settings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

typealias BiConsumer<T> = (T, T) -> Unit

class ObservableManager<T> {
    private val properties = mutableMapOf<String, Any>()
    private val listeners = mutableMapOf<String, MutableList<BiConsumer<Any>>>()

    private fun updateValue(name: String, old: Any, new: Any) {
        if (old != new) {
            listeners[name]?.forEach { it(old, new) }
        }
    }

    private fun updateValue(name: String, value: Any) {
        updateValue(name, properties.put(name, value)!!, value)
    }

    fun registerListener(name: String, biConsumer: BiConsumer<Any>) {
        listeners.getOrPut(name) { mutableListOf() }.add(biConsumer)
        properties[name]?.let { biConsumer(it, it) }
    }

    fun removeListeners(name: String) {
        properties.remove(name)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <V : Any> registerListener(property: KProperty1<T, V>, crossinline consumer: BiConsumer<V>) =
        registerListener(property.name) { old, new -> consumer(old as V, new as V) }

    inline fun <reified V : Any> removeListeners(property: KProperty1<T, V>) =
        removeListeners(property.name)

    inner class WrappingRWProperty<V : Any>(
        prop: KProperty<*>,
        value: V
    ) : ReadWriteProperty<T, V> {

        init {
            properties[prop.name] = value
        }

        @Suppress("UNCHECKED_CAST")
        override operator fun getValue(thisRef: T, property: KProperty<*>) = properties[property.name] as V

        override operator fun setValue(thisRef: T, property: KProperty<*>, value: V) =
            updateValue(property.name, value)
    }

    inner class WrappingDelegateRWProperty<V : Any>(
        private val delegate: KMutableProperty0<V>
    ) : ReadWriteProperty<T, V> {

        override operator fun getValue(thisRef: T, property: KProperty<*>) = delegate.get()

        override operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            val old = delegate.get()
            delegate.set(value)
            updateValue(property.name, old, value)
        }
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
    private val value: V
) : DelegateProvider<T, V> {
    override operator fun provideDelegate(
        thisRef: T,
        prop: KProperty<*>
    ): ReadWriteProperty<T, V> = delegate.WrappingRWProperty(prop, value)
}

class ObservablePropertyValue<T, V : Any>(
    private val delegate: ObservableManager<T>,
    private val property: KMutableProperty0<V>
) : DelegateProvider<T, V> {
    override operator fun provideDelegate(
        thisRef: T,
        prop: KProperty<*>
    ): ReadWriteProperty<T, V> = delegate.WrappingDelegateRWProperty(property)
}

interface Observable<T> {
    val manager: ObservableManager<T>
}

open class DefaultObservable<T> : Observable<T> {
    override val manager = ObservableManager<T>()
}

fun <T, V : Any> Observable<T>.observable(value: V): DelegateProvider<T, V> =
    ObservableValue(manager, value)

fun <T, V : Any> Observable<T>.observable(prop: KMutableProperty0<V>): DelegateProvider<T, V> =
    ObservablePropertyValue(manager, prop)

inline fun <T, V : Any> Observable<T>.registerListener(
    property: KProperty1<T, V>,
    crossinline consumer: BiConsumer<V>
) = manager.registerListener(property, consumer)

inline fun <T, reified V : Any> Observable<T>.removeListeners(
    property: KProperty1<T, V>
) = manager.removeListeners(property)
