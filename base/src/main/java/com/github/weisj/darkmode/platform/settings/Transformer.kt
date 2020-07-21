package com.github.weisj.darkmode.platform.settings

class Transformer<R, T>(
    val write: (T) -> R,
    val read: (R) -> T
)

fun <T> constantTransformer(): Transformer<T, T> = Transformer({ t -> t }, { t -> t })
