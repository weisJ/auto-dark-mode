package com.github.weisj.darkmode.platform.settings

import java.lang.NullPointerException

inline fun <reified T> Any.castSafelyTo() : T? = (this as? T)

fun String.toPair(delimiter: Char): Pair<String, String>? = split(delimiter, limit = 2).let {
    if (it.size == 2) it[0] to it[1] else null
}

infix fun <R, T, S> ((R) -> T).andThen(g: (T) -> S): (R) -> S {
    return { r -> g(this(r)) }
}

fun <R, T>((R) -> T).or(fallback : T) : (R?) -> T = {r -> r?.let { this(r) }?:fallback }

fun <T> Lazy<T?>.assertNonNull(message : String = "Lazy value is null") : Lazy<T> = lazy {
    if (value == null) throw NullPointerException(message)
    value!!
}
