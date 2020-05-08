import UberJniJarPlugin.asVariantName

plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.cpp-language")
    `uber-jni-jar`
}

library {
    dependencies {
        jvmImplementation(project(":auto-dark-mode-base"))
        jvmImplementation("com.github.weisj:darklaf-native-utils")
    }
    targetMachines.addAll(machines.windows.x86, machines.windows.x86_64)
    variants.configureEach {
        resourcePath.set("com/github/weisj/darkmode/${project.name}/${asVariantName(targetMachine)}")
        sharedLibrary {
            compileTasks.configureEach {
                compilerArgs.addAll(toolChain.map {
                    when (it) {
                        is Gcc, is Clang -> listOf("--std=c++11")
                        is VisualCpp -> listOf("/EHsc")
                        else -> emptyList()
                    }
                })

                // Build type not modeled yet, assuming release
                compilerArgs.addAll(toolChain.map {
                    when (it) {
                        is Gcc, is Clang -> listOf("-O2")
                        is VisualCpp -> listOf("/O2")
                        else -> emptyList()
                    }
                })
            }
            linkTask.configure {
                linkerArgs.addAll(toolChain.map {
                    when (it) {
                        is Gcc, is Clang -> listOf("-luser32", "-ladvapi32")
                        is VisualCpp -> listOf("user32.lib", "Advapi32.lib")
                        else -> emptyList()
                    }
                })
            }
        }
    }
}
