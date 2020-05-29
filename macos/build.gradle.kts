import JniUtils.asVariantName

plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.objective-cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
}

library {
    val minOs = "10.14"

    dependencies {
        jvmImplementation(project(":auto-dark-mode-base"))
        jvmImplementation("com.github.weisj:darklaf-native-utils")
        nativeImplementation("dev.nokee.framework:JavaVM:[$minOs,)")
        nativeImplementation("dev.nokee.framework:JavaVM:[$minOs,)") {
            capabilities {
                requireCapability("JavaVM:JavaNativeFoundation:[$minOs,)")
            }
        }
        nativeImplementation("dev.nokee.framework:AppKit:[$minOs,)")
    }

    targetMachines.addAll(machines.macOS.x86_64)
    variants.configureEach {
        resourcePath.set("com/github/weisj/darkmode/${project.name}/${asVariantName(targetMachine)}")
        sharedLibrary {
            compileTasks.configureEach {
                compilerArgs.addAll("-mmacosx-version-min=$minOs")
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
                linkerArgs.addAll("-lobjc", "-mmacosx-version-min=$minOs")
            }
        }
    }
}
