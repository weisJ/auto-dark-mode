import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import java.util.concurrent.TimeUnit

plugins {
    java
    id("com.google.devtools.ksp")
    `use-prebuilt-binaries`
    kotlin("jvm")
}

dependencies {
    implementation(projects.autoDarkModeBase)
    implementation(libs.darklaf.nativeUtils)

    ksp(libs.autoservice.processor)
    implementation(libs.autoservice.annotations)
    compileOnly(kotlin("stdlib"))
}

// Generate JNI headers alongside normal Java compilation
tasks.named<JavaCompile>("compileJava") {
    options.headerOutputDirectory.set(layout.buildDirectory.dir("generated/jni"))
}

val javaHome = Jvm.current().javaHome
val jniHeaderDir = layout.buildDirectory.dir("generated/jni")
val srcDir = file("src/main/cpp")
val headersDir = file("src/main/headers")

val variantName = "linux-x86-64"
val outputFile = layout.buildDirectory.file("lib/main/release/$variantName/libauto-dark-mode-linux-gtk.so")

fun runCommand(vararg command: String): List<String> =
    try {
        val proc = ProcessBuilder(*command).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor(10, TimeUnit.SECONDS)
        if (proc.exitValue() == 0) out.split("\\s+".toRegex()).filter { it.isNotEmpty() } else emptyList()
    } catch (_: Exception) {
        emptyList()
    }

// Evaluated lazily; pkg-config returns empty lists on non-Linux platforms where it is absent.
val gtkCFlags: List<String> by lazy {
    runCommand("pkg-config", "--cflags", "glibmm-2.4", "giomm-2.4", "gtkmm-3.0", "sigc++-2.0", "gtk+-3.0")
}
val gtkLibs: List<String> by lazy {
    runCommand("pkg-config", "--libs", "glibmm-2.4", "giomm-2.4", "gtkmm-3.0", "sigc++-2.0", "gtk+-3.0")
}

val compileNativeLinuxX8664 =
    tasks.register("compileNativeLinuxX8664", Exec::class) {
        onlyIf { OperatingSystem.current().isLinux }
        dependsOn(tasks.named("compileJava"))

        inputs.dir(srcDir)
        inputs.dir(headersDir)
        inputs.dir(jniHeaderDir)
        outputs.file(outputFile)

        doFirst {
            outputFile.get().asFile.parentFile.mkdirs()
        }

        val srcFiles = fileTree(srcDir) { include("**/*.cpp") }.files.map { it.absolutePath }

        commandLine(
            buildList {
                add("g++")
                add("--std=c++11")
                add("-O2")
                add("-shared")
                add("-fPIC")
                addAll(gtkCFlags)
                add("-I${javaHome}/include")
                add("-I${javaHome}/include/linux")
                add("-I${jniHeaderDir.get()}")
                add("-I$headersDir")
                add("-o"); add(outputFile.get().asFile.absolutePath)
                addAll(srcFiles)
                addAll(gtkLibs)
            },
        )
    }

// Include the native compile task in the standard build lifecycle
tasks.named("build") { dependsOn(compileNativeLinuxX8664) }

prebuiltBinaries {
    alwaysUsePrebuiltArtifact = true
    resourcePath = "com/github/weisj/darkmode/${project.name}"
    variants =
        listOf(
            StubJniLibrary(
                operatingSystem = "linux",
                architecture = "x86-64",
            ),
        )
}

// Wire the compile task output to the prebuilt-binaries download task so that locally
// built .so files are used when building on Linux and are embedded in the JAR.
afterEvaluate {
    val downloadTaskName = "downloadPrebuiltBinary$variantName"
    if (tasks.names.contains(downloadTaskName)) {
        tasks.named(downloadTaskName, DownloadPrebuiltBinariesTask::class.java) {
            dependsOn(compileNativeLinuxX8664)
            localNativeLibraryPath.set(outputFile.map { it.asFile.absolutePath })
        }
    }
}

