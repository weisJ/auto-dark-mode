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

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

/**
 * Cast the property to the specified type if applicable for read and write operations.
 */
inline fun <reified T, R : Any> KMutableProperty0<R>.withType(): KMutableProperty0<T>? {
    return if (get().javaClass.kotlin == T::class) {
        this.castSafelyTo<KMutableProperty0<T>>()
    } else {
        null
    }
}

/**
 * Cast the property to the specified type if applicable for read operations.
 */
inline fun <reified T, R : Any> KMutableProperty0<R>.withOutType(): KMutableProperty0<T>? {
    return if (T::class.java.isAssignableFrom(get().javaClass)) {
        this.castSafelyTo<KMutableProperty0<T>>()
    } else {
        null
    }
}

/**
 * Cast the property to the specified type if applicable for write operations.
 */
inline fun <reified T, R : Any> KMutableProperty0<R>.withInType(): KMutableProperty0<T>? {
    return if (get().javaClass.isAssignableFrom(T::class.java)) {
        this.castSafelyTo<KMutableProperty0<T>>()
    } else {
        null
    }
}

operator fun <T> KMutableProperty0<T>.setValue(target: Any?, property: KProperty<*>, value: T) = set(value)

operator fun <T> KProperty0<T>.getValue(target: Any?, property: KProperty<*>): T = get()
