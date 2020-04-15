import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

intellij {
    version = "2020.1"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        <ul>
            <li>Support for macOS.</li>
            <li>Improved lightweight polling system.</li>
            <li>Removed use of Light Services as they currently block dynamic plugin loading.</li>
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
