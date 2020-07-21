package com.github.weisj.darkmode.platform.settings

fun String.toPair(delimiter: Char): Pair<String, String>? = split(delimiter, limit = 2).let {
    if (it.size == 2) it[0] to it[1] else null
}
