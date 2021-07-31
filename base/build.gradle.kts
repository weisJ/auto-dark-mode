plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    val ideaVersion = rootProject.extra["idea.version"]
    compileOnly("com.jetbrains.intellij.platform:util:$ideaVersion")
    api(libs.darklaf.nativeUtils)
    compileOnly(kotlin("stdlib-jdk8"))
}
