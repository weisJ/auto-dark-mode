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

import kotlin.reflect.KProperty

/**
 * A mutable property delegate that holds an optional value.
 * When the value is `null`, the [fallback] lambda is invoked lazily to compute
 * and cache a default value.  Assigning a new value (including `null` via the
 * backing field) replaces the cached result so that [fallback] can be called
 * again on the next read.
 *
 * Usage:
 * ```kotlin
 * private var darkTheme: UIThemeLookAndFeelInfo by lazyFallback { DefaultLaf.DARK.info() }
 * ```
 */
class LazyFallback<T : Any>(private val fallback: () -> T) {
    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: fallback().also { value = it }
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}

/** Creates a [LazyFallback] delegate with the given [fallback] producer. */
fun <T : Any> lazyFallback(fallback: () -> T): LazyFallback<T> = LazyFallback(fallback)

