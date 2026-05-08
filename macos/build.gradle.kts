import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem

plugins {
    java
    kotlin("jvm")
    id("com.google.devtools.ksp")
    `use-prebuilt-binaries`
}

prebuiltBinaries {
    alwaysUsePrebuiltArtifact = true
    resourcePath = "com/github/weisj/darkmode/${project.name}"
    variants =
        listOf(
            StubJniLibrary(
                operatingSystem = "macos",
                architecture = "x86-64",
            ),
            StubJniLibrary(
                operatingSystem = "macos",
                architecture = "arm64",
            ),
        )
}

dependencies {
    implementation(libs.autoservice.annotations)
    implementation(projects.autoDarkModeBase)
    implementation(libs.darklaf.nativeUtils)
    compileOnly(kotlin("stdlib"))
    ksp(libs.autoservice.processor)
}

// Generate JNI headers alongside normal Java compilation
tasks.named<JavaCompile>("compileJava") {
    options.headerOutputDirectory.set(layout.buildDirectory.dir("generated/jni"))
}

data class MacOSTarget(val arch: String, val minOs: String, val clangTarget: String)

val macOSTargets =
    listOf(
        MacOSTarget(arch = "x86-64", minOs = "10.10", clangTarget = "x86_64-apple-macos10.10"),
        MacOSTarget(arch = "arm64", minOs = "11", clangTarget = "arm64-apple-macos11"),
    )

val javaHome = Jvm.current().javaHome
val jniHeaderDir = layout.buildDirectory.dir("generated/jni")
val objcppSrcDir = file("src/main/objcpp")

macOSTargets.forEach { target ->
    val variantName = "macos-${target.arch}"
    val outputFile = layout.buildDirectory.file("lib/main/release/$variantName/lib${project.name}.dylib")

    val compileTask =
        tasks.register("compileNativeMacos${target.arch.toTaskSuffix()}", Exec::class) {
            onlyIf { OperatingSystem.current().isMacOsX }
            dependsOn(tasks.named("compileJava"))

            inputs.dir(objcppSrcDir)
            inputs.dir(jniHeaderDir)
            outputs.file(outputFile)

            doFirst {
                outputFile.get().asFile.parentFile.mkdirs()
            }

            commandLine(
                "clang++",
                "-dynamiclib",
                "-std=c++17",
                "-O2",
                "-mmacosx-version-min=${target.minOs}",
                "-target", target.clangTarget,
                "-I${javaHome}/include",
                "-I${javaHome}/include/darwin",
                "-I${jniHeaderDir.get()}",
                "-I$objcppSrcDir",
                "-lobjc",
                "-framework", "AppKit",
                "-framework", "Cocoa",
                "-framework", "Foundation",
                "-o", outputFile.get().asFile.absolutePath,
                "$objcppSrcDir/DarkModeMacOS.mm",
                "$objcppSrcDir/JNFUtils.mm",
            )
        }

    tasks.named("build") { dependsOn(compileTask) }
}

// Wire the macOS Exec task outputs to the prebuilt-binaries download tasks so that locally
// built dylibs are packaged into the JAR and GitHub downloads are skipped on macOS.
afterEvaluate {
    macOSTargets.forEach { target ->
        val variantName = "macos-${target.arch}"
        val outputFile = layout.buildDirectory.file("lib/main/release/$variantName/lib${project.name}.dylib")
        val compileTaskName = "compileNativeMacos${target.arch.toTaskSuffix()}"
        tasks.named("downloadPrebuiltBinary$variantName", DownloadPrebuiltBinariesTask::class.java) {
            dependsOn(tasks.named(compileTaskName))
            localNativeLibraryPath.set(outputFile.map { it.asFile.absolutePath })
        }
    }
}

