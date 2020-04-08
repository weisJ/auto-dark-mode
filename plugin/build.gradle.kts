import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `jni-library`
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

intellij {
    version = "2019.3"
}

tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        <ul>
            <li>Improved lightweight polling system.</li>
            <li>Removed use of Light Services as they currently block dynamic plugin loading.</li>
        </ul>
        """
    )
}

fun DependencyHandlerScope.javaImplementation(dep: Any) {
    compileOnly(dep)
    runtimeOnly(dep)
}

dependencies {
    javaImplementation("com.github.weisj:darklaf-native-utils")
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
