package com.github.weisj.darkmode.platform.settings

import kotlin.reflect.KMutableProperty0

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
