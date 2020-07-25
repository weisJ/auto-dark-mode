import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.PublishTask

plugins {
    id("org.jetbrains.intellij")
    id("com.github.vlsi.gradle-extensions")
    kotlin("jvm")
}

val isPublished by props()
val intellijPublishToken: String by props("")

intellij {
    version = "2020.1"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        <ul>
            <li>Added option to choose editor theme independent from IDE theme.</li>
        </ul>
        """
    )
    sinceBuild("201")
    untilBuild("201.*")
}

dependencies {
    implementation(project(":auto-dark-mode-base"))
    implementation(project(":auto-dark-mode-windows"))
    implementation(project(":auto-dark-mode-macos"))
    implementation(project(":auto-dark-mode-linux"))
}

tasks.withType<PublishTask> {
    token(intellijPublishToken)
    if (version.toString().contains("pre")) {
        channels("pre-release")
    }
}

tasks.buildPlugin {
    project.rootProject.subprojects.forEach {
        dependsOn(it.tasks.withType<Jar>())
    }
}

listOf(BuildSearchableOptionsTask::class, PrepareSandboxTask::class)
    .forEach { tasks.withType(it).configureEach { enabled = isPublished } }
