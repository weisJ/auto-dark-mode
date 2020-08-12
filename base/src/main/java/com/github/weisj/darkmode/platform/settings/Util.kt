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

inline fun <reified T> Any.castSafelyTo(): T? = (this as? T)

fun String.toPair(delimiter: Char): Pair<String, String>? = split(delimiter, limit = 2).let {
    if (it.size == 2) it[0] to it[1] else null
}

infix fun <R, T, S> ((R) -> T).andThen(g: (T) -> S): (R) -> S {
    return { r -> g(this(r)) }
}

fun <R, T> ((R) -> T).or(fallback: T): (R?) -> T = { r -> r?.let { this(r) } ?: fallback }

fun <T> Lazy<T?>.assertNonNull(message: String = "Lazy value is null"): Lazy<T> = lazy {
    if (value == null) throw NullPointerException(message)
    value!!
}

fun <T> Lazy<T>.ifPresent(block: (T) -> Unit) {
    if (isInitialized()) block(value)
}

fun <T> Lazy<T>.letValue(block: (T) -> Unit) {
    block(value)
}

fun <T, K> Lazy<T>.map(block: (T) -> K): Lazy<K> = lazy { block(value) }

fun <T, K> Lazy<T>.lazyCall(block: T.() -> Lazy<K>): Lazy<K> = lazy { block(this.value).value }
