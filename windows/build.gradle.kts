import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem

plugins {
    java
    `use-prebuilt-binaries`
    kotlin("jvm")
}

dependencies {
    implementation(projects.autoDarkModeBase)
    compileOnly(kotlin("stdlib-jdk8"))
}

// Generate JNI headers alongside normal Java compilation
tasks.named<JavaCompile>("compileJava") {
    options.headerOutputDirectory.set(layout.buildDirectory.dir("generated/jni"))
}

val javaHome = Jvm.current().javaHome
val jniHeaderDir = layout.buildDirectory.dir("generated/jni")
val srcFile = file("src/main/cpp/DarkModeWindows.cpp")

data class WindowsTarget(val arch: String)

// Converts e.g. "x86-64" -> "X8664", "x86" -> "X86" for use in task names.
fun String.toTaskSuffix() = replace("-", "").replaceFirstChar { it.uppercase() }

val windowsTargets =
    listOf(
        WindowsTarget(arch = "x86-64"),
        WindowsTarget(arch = "x86"),
    )

windowsTargets.forEach { target ->
    val variantName = "windows-${target.arch}"
    val outputFile = layout.buildDirectory.file("lib/main/release/$variantName/auto-dark-mode-windows.dll")

    val compileTask =
        tasks.register("compileNativeWindows${target.arch.toTaskSuffix()}", Exec::class) {
            onlyIf { OperatingSystem.current().isWindows }
            dependsOn(tasks.named("compileJava"))

            inputs.files(srcFile)
            inputs.dir(jniHeaderDir)
            outputs.file(outputFile)

            doFirst {
                outputFile.get().asFile.parentFile.mkdirs()
            }

            commandLine(
                "cl.exe",
                "/std:c++17", "/EHsc", "/W4", "/O2",
                "/LD",
                "/I${javaHome}/include",
                "/I${javaHome}/include/win32",
                "/I${jniHeaderDir.get()}",
                "/Fe${outputFile.get().asFile.absolutePath}",
                "$srcFile",
                "user32.lib", "Advapi32.lib",
            )
        }

    // Include the native compile task in the standard build lifecycle
    tasks.named("build") { dependsOn(compileTask) }
}

prebuiltBinaries {
    alwaysUsePrebuiltArtifact = true
    resourcePath = "com/github/weisj/darkmode/${project.name}"
    variants =
        listOf(
            StubJniLibrary(
                operatingSystem = "windows",
                architecture = "x86",
            ),
            StubJniLibrary(
                operatingSystem = "windows",
                architecture = "x86-64",
            ),
        )
}

// Wire compile-task outputs to the prebuilt-binaries download tasks so that locally
// built DLLs are used when building on Windows and are embedded in the JAR.
afterEvaluate {
    windowsTargets.forEach { target ->
        val variantName = "windows-${target.arch}"
        val outputFile = layout.buildDirectory.file("lib/main/release/$variantName/auto-dark-mode-windows.dll")
        val compileTaskName = "compileNativeWindows${target.arch.toTaskSuffix()}"
        val downloadTaskName = "downloadPrebuiltBinary$variantName"
        if (tasks.names.contains(downloadTaskName)) {
            tasks.named(downloadTaskName, DownloadPrebuiltBinariesTask::class.java) {
                dependsOn(tasks.named(compileTaskName))
                localNativeLibraryPath.set(outputFile.map { it.asFile.absolutePath })
            }
        }
    }
}

