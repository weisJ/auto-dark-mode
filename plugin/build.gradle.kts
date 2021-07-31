import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask

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
    version.set("ideaPlugin".v)
}

tasks.withType<PatchPluginXmlTask> {
    changeNotes.set(
        """
        v1.5.4
        <ul>
            <li>Fix segmentation fault on GNOME.</li>
            <li>Fix theme not updating with OS theme on GNOME.</li>
        </ul>
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
    sinceBuild.set("ideaPlugin.since".v)
    untilBuild.set("ideaPlugin.until".v)
}

dependencies {
    implementation(projects.autoDarkModeBase)
    implementation(projects.autoDarkModeWindows)
    implementation(projects.autoDarkModeMacos)
    implementation(projects.autoDarkModeLinux)
    implementation(kotlin("reflect"))

    kapt(libs.autoservice.processor)
    compileOnly(libs.autoservice.annotations)

    testImplementation(projects.autoDarkModeLinuxGnome)
    testImplementation(libs.test.junit.api)
    testRuntimeOnly(libs.test.junit.engine)
}

tasks {
    test {
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
        }
        useJUnitPlatform()
    }

    publishPlugin {
        token.set(intellijPublishToken)
        if (version.toString().contains("pre")) {
            channels.add("pre-release")
        }
    }
}

listOf(BuildSearchableOptionsTask::class, PrepareSandboxTask::class)
    .forEach { tasks.withType(it).configureEach { enabled = isPublished } }
