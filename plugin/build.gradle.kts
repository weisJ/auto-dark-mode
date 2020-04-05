import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `jni-library`
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

intellij {
    version = "2019.3.4"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        Initial version. Change the IDEA theme based on Windows settings.
      """
    )
}

fun DependencyHandlerScope.javaImplementation(dep: Any) {
    compileOnly(dep)
    runtimeOnly(dep)
}

dependencies {
    javaImplementation(kotlin("stdlib-jdk8"))
    javaImplementation(kotlin("reflect"))
}

tasks.jar {
    CrLfSpec(LineEndings.LF).run {
        into("META-INF") {
            filteringCharset = "UTF-8"
            textFrom("$rootDir/licenses/NATIVEUTIL_LICENSE.txt")
        }
    }
}

library {
    targetMachines.addAll(machines.windows.x86, machines.windows.x86_64)
    binaries.whenElementFinalized(CppSharedLibrary::class) {
        linkTask.get().linkerArgs.addAll(
            when (toolChain) {
                is Gcc, is Clang -> listOf("-luser32", "-ladvapi32")
                is VisualCpp -> listOf("user32.lib", "Advapi32.lib")
                else -> emptyList()
            }
        )
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
