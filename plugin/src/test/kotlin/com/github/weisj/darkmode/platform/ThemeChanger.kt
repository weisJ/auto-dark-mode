package com.github.weisj.darkmode.platform

import java.util.concurrent.TimeUnit

interface ThemeChanger {
    val currentTheme: String

    fun String.runCommand(): String {
        val process = ProcessBuilder(*split(" ").toTypedArray()).start()
        val output = process.inputStream.reader(Charsets.UTF_8).use {
            it.readText()
        }
        process.waitFor(10, TimeUnit.SECONDS)
        return output
    }

    fun String.stripQuotes(): String {
        return if (startsWith("'")) {
            trim().drop(1).dropLast(1)
        } else trim()
    }
}
