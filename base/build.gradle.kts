plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    compileOnly("com.jetbrains.intellij.platform:util")
    api("com.github.weisj:darklaf-native-utils")
}
