import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.PublishTask

plugins {
    id("org.jetbrains.intellij")
    id("com.github.vlsi.gradle-extensions")
    kotlin("jvm")
    kotlin("kapt")
}

val String.v: String get() = rootProject.extra["$this.version"] as String
val isPublished by props(true)
val intellijPublishToken: String by props("")

intellij {
    version = "ideaPlugin".v
}

tasks.withType<PatchPluginXmlTask> {
    changeNotes(
        """
        v1.5.2
        <ul>
            <li>Notify macOS users to restart IDEA on installation</li>
        </ul>
        v1.5.1
        <ul>
            <li>Made dark mode detection more reliable.</li>
            <li>Improved compatibility with third party tools such as NightOwl.</li>
        </ul>
        v1.5.0
        <ul>
            <li>Added option to individually choose whether the IDE/editor theme gets changed.</li>
        </ul>
        """
    )
    sinceBuild("ideaPlugin.since".v)
    untilBuild("ideaPlugin.until".v)
}

dependencies {
    implementation(project(":auto-dark-mode-base"))
    implementation(project(":auto-dark-mode-windows"))
    implementation(project(":auto-dark-mode-macos"))
    implementation(project(":auto-dark-mode-linux"))

    kapt(platform(project(":auto-dark-mode-dependencies-bom")))
    kapt("com.google.auto.service:auto-service")
    compileOnly("com.google.auto.service:auto-service-annotations")

    testImplementation(project(":auto-dark-mode-linux-gnome"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

tasks.test {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
    useJUnitPlatform()
}

tasks.withType<PublishTask> {
    token(intellijPublishToken)
    if (version.toString().contains("pre")) {
        channels("pre-release")
    }
}

listOf(BuildSearchableOptionsTask::class, PrepareSandboxTask::class)
    .forEach { tasks.withType(it).configureEach { enabled = isPublished } }
