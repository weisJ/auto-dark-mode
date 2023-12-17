plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
    kotlin("jvm")
}

dependencies {
    implementation(projects.autoDarkModeBase)
    compileOnly(kotlin("stdlib-jdk8"))
}

library {
    targetMachines.addAll(machines.windows.x86, machines.windows.x86_64)
    variants.configureEach {
        resourcePath.set("com/github/weisj/darkmode/${project.name}")
        sharedLibrary {
            compileTasks.configureEach {
                compilerArgs.addAll(
                    toolChain.map {
                        when (it) {
                            is Gcc, is Clang -> listOf(
                                "--std=c++17",
                                "-Wall",
                                "-Wextra",
                                "-pedantic",
                                "-Wno-language-extension-token",
                                "-Wno-ignored-attributes"
                            )
                            is VisualCpp -> listOf("/std:c++17", "/EHsc", "/W4", "/permissive", "/WX")
                            else -> emptyList()
                        }
                    }
                )
                optimizedBinary()
            }
            linkTask.configure {
                linkerArgs.addAll(
                    toolChain.map {
                        when (it) {
                            is Gcc, is Clang -> listOf("-luser32", "-ladvapi32")
                            is VisualCpp -> listOf("user32.lib", "Advapi32.lib")
                            else -> emptyList()
                        }
                    }
                )
            }
        }
    }
}
