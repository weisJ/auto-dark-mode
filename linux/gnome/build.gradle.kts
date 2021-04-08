import JniUtils.asVariantName

plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
    kotlin("jvm")
    kotlin("kapt")
}

library {
    dependencies {
        jvmImplementation(project(":auto-dark-mode-base"))
        jvmImplementation("com.github.weisj:darklaf-native-utils")
    }
    targetMachines.addAll(machines.linux.x86_64)
    variants.configureEach {
        resourcePath.set("com/github/weisj/darkmode/${project.name}/${asVariantName(targetMachine)}")
        sharedLibrary {
            compileTasks.configureEach {
                compilerArgs.add("--std=c++11")
                compilerArgs.addAll(toolChain.map {
                    when (it) {
                        is Gcc, is Clang -> compilerFlagsFor(
                            "glibmm-2.4",
                            "giomm-2.4",
                            "sigc++-2.0",
                            "gtk+-3.0"
                        )
                        else -> emptyList()
                    }
                })

                // Build type not modeled yet, assuming release
                compilerArgs.addAll(toolChain.map {
                    when (it) {
                        is Gcc, is Clang -> listOf("-O2")
                        else -> emptyList()
                    }
                })
            }
            linkTask.configure {
                linkerArgs.addAll(toolChain.map {
                    when (it) {
                        is Gcc, is Clang -> linkerFlagsFor(
                            "glibmm-2.4",
                            "giomm-2.4",
                            "sigc++-2.0",
                            "gtk+-3.0"
                        )
                        else -> emptyList()
                    }
                })
            }
        }
    }
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    kapt(platform(project(":auto-dark-mode-dependencies-bom")))
    kapt("com.google.auto.service:auto-service")
    compileOnly("com.google.auto.service:auto-service-annotations")
}

fun compilerFlagsFor(vararg packages: String): List<String> =
    "pkg-config --cflags ${packages.joinToString(separator = " ")}".runCommand().split(" ").distinct()

fun linkerFlagsFor(vararg packages: String): List<String> =
    "pkg-config --libs ${packages.joinToString(separator = " ")}".runCommand().split(" ").distinct()

fun String.runCommand(): String {
    val process = ProcessBuilder(*split(" ").toTypedArray()).start()
    val output = process.inputStream.reader(Charsets.UTF_8).use {
        it.readText()
    }
    process.waitFor(10, TimeUnit.SECONDS)
    return output.trim()
}
