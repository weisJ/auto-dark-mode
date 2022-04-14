package com.github.weisj.darkmode.platform

import java.lang.ClassLoader

/**
 * Runs the given function using the specified class loader
 * @param contextClassLoader the context class loader that should be used instead of the current one
 * @param function function to be executed using the specified class loader
 */
fun <T> withContextClassLoader(contextClassLoader: ClassLoader, function: () -> T): T {
    val currentLoader = Thread.currentThread().contextClassLoader
    val value: T
    try {
        Thread.currentThread().contextClassLoader = contextClassLoader
        value = function()
    } finally {
        Thread.currentThread().contextClassLoader = currentLoader
    }

    return value
}
