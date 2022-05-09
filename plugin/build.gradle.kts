import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask

plugins {
    id("org.jetbrains.intellij")
    id("com.github.vlsi.gradle-extensions")
    id("com.google.devtools.ksp")
    kotlin("jvm")
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
        v1.7.0
        <ul>
            <li>Generic GTK desktops support. (XSettings based)</li>
        </ul>
        v1.6.2
        <ul>
            <li>Detect more GTK themes on GNOME desktops.</li>
            <li>Stability improvements on macOS</li>
        </ul>
        v.1.6.1
        <ul>
            <li>Fixed native library loading on macOS.</li>
        </ul>
        v1.6.0
        <ul>
            <li>M1 support for macOS.</li>
        </ul>
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

    ksp(libs.autoservice.processor)
    implementation(libs.autoservice.annotations)
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    testImplementation(projects.autoDarkModeLinuxGtk)
    testImplementation(projects.autoDarkModeWindows)
    testImplementation(projects.autoDarkModeMacos)
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
