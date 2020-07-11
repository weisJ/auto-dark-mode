import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij")
    kotlin("jvm")
}

intellij {
    version = "2019.3"
    updateSinceUntilBuild = false
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        <ul>
            <li>Added option to choose editor theme independent from IDE theme.</li>
        </ul>
        """
    )
    sinceBuild("193")
    untilBuild("201.*")
}

dependencies {
    implementation(project(":auto-dark-mode-base"))
    implementation(project(":auto-dark-mode-windows"))
    implementation(project(":auto-dark-mode-macos"))
}

tasks.buildPlugin {
    dependsOn(project.rootProject.tasks.jar)
}

tasks.buildPlugin {
    project.rootProject.subprojects.forEach {
        dependsOn(it.tasks.withType<Jar>())
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
