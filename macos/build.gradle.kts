import JniUtils.asVariantName

plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.objective-cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
    kotlin("jvm")
    kotlin("kapt")
}

library {
    val minOs = "10.14"
    val frameworkVersion = "[10.15,)"

    dependencies {
        jvmImplementation(project(":auto-dark-mode-base"))
        nativeImplementation("dev.nokee.framework:JavaNativeFoundation:$frameworkVersion")
        nativeImplementation("dev.nokee.framework:AppKit:$frameworkVersion")
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

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    kapt(platform(project(":auto-dark-mode-dependencies-bom")))
    kapt("com.google.auto.service:auto-service")
    compileOnly("com.google.auto.service:auto-service-annotations")
}
