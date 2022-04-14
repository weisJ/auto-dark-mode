plugins {
    java
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(projects.autoDarkModeBase)
    compileOnly(libs.linux.dbus.core) {
        exclude(group = "org.slf4j")
    }
    compileOnly(libs.linux.dbus.transport) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.kotlinx.coroutines.core.jvm)

    ksp(libs.autoservice.processor)
    compileOnly(libs.autoservice.annotations)
    compileOnly(kotlin("stdlib-jdk8"))
}
