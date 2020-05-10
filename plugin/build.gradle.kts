import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij")
    kotlin("jvm")
}

intellij {
    version = "2020.1"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        <ul>
            <li>Reliable dark mode detection on Catalina.</li>
        </ul>
        """
    )
}

tasks.buildPlugin {
    project.rootProject.subprojects.forEach {
        dependsOn(it.tasks.withType<Jar>())
    }
}

dependencies {
    implementation(project(":auto-dark-mode-base"))
    implementation(project(":auto-dark-mode-windows"))
    implementation(project(":auto-dark-mode-macos"))
}

tasks.buildPlugin {
    dependsOn(project.rootProject.tasks.jar)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
