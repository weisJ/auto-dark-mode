package com.github.weisj.darkmode.platform

class OneTimeAction(private val action : () -> Unit) {
    var executed : Boolean = false

    operator fun invoke() {
        if (!executed) {
            action()
            executed = true
        }
    }
}
