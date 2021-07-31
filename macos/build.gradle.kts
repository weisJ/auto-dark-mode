plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
    id("dev.nokee.jni-library")
    id("dev.nokee.objective-cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
    `apple-m1-toolchain`
}

val jnfConfig: Configuration by configurations.creating {
    attributes {
        attribute(Attribute.of("dev.nokee.architecture", String::class.java), "arm64")
    }
}

val nativeResourcePath = "com/github/weisj/darkmode/${project.name}"

tasks.jar {
    jnfConfig.asFileTree.forEach {
        from(zipTree(it)) {
            into("$nativeResourcePath/JavaNativeFoundation.framework")
            include("JavaNativeFoundation*")
            exclude("**/*.tbd")
        }
    }
}

dependencies {
    jnfConfig(libs.macos.javaNativeFoundation)
    kapt(libs.autoservice.processor)
    compileOnly(libs.autoservice.annotations)
    compileOnly(kotlin("stdlib-jdk8"))
}

library {
    dependencies {
        jvmImplementation(projects.autoDarkModeBase)
        jvmLibImplementation(libs.darklaf.nativeUtils)
        nativeLibImplementation(libs.macos.javaNativeFoundation)
        nativeLibImplementation(libs.macos.appKit)
    }
    targetMachines.addAll(machines.macOS.x86_64, machines.macOS.architecture("arm64"))
    variants.configureEach {
        resourcePath.set("$nativeResourcePath/${targetMachine.variantName}")
        sharedLibrary {
            val isArm = targetMachine.architectureString == "arm64"
            val minOs = if (isArm) "11" else "10.10"
            compileTasks.configureEach {
                compilerArgs.addAll("-mmacosx-version-min=$minOs")
                // Build type not modeled yet, assuming release
                optimizedBinary()
            }
            linkTask.configure {
                val systemFrameworks = "/System/Library/Frameworks"
                val current = "Versions/Current"
                val jnfName = "JavaNativeFoundation.framework"
                linkerArgs.addAll(
                    "-lobjc", "-mmacosx-version-min=$minOs",
                    // "-framework", "AppKit",
                    // "-framework", "Cocoa",
                    // The custom JNF framework specified @rpath for searching. As we aren't actually linking
                    // with the dynamic library of the framework we specifically have to add the system framework
                    // search paths accordingly.
                    // First try any system provided framework (this will fail on arm64):
                    "-rpath", "$systemFrameworks/$jnfName/$current",
                    "-rpath", "$systemFrameworks/JavaVM.framework/$current/Frameworks/$jnfName/$current",
                    // Then try the jdk provided framework (folder layout may vary. We check multiple possibilities):
                    "-rpath", "@executable_path/../lib/$jnfName",
                    "-rpath", "@executable_path/../lib/$jnfName/$current/",
                    "-rpath", "@executable_path/../lib/$jnfName/Versions/A/",
                    // Lastly use our bundled drop-in replacement:
                    "-rpath", "@loader_path/$jnfName"
                )
            }
        }
    }
}
