plugins {
    `jni-library`
    id("org.jetbrains.intellij")
}

fun DependencyHandlerScope.javaImplementation(dep: Any) {
    compileOnly(dep)
    runtimeOnly(dep)
}

dependencies {
    javaImplementation(project(":auto-dark-mode-base"))
    javaImplementation("com.github.weisj:darklaf-native-utils")
}

val macPath by tasks.registering(MacOSSdkPathTask::class)

val sdkRoot: Provider<String> get() = macPath.map { it.sdkPath.absolutePath }

fun ListProperty<String>.addJavaFrameworks() {
    addAll("-framework", "JavaNativeFoundation")
    add("-F")
    add(sdkRoot.map { "$it/System/Library/Frameworks/JavaVM.framework/Frameworks" })
    add("-F")
    add("/System/Library/Frameworks/JavaVM.framework/Frameworks")
}

library {
    targetMachines.addAll(machines.macOS.x86_64)
    binaries.configureEach {
        compileTask.get().apply {
            dependsOn(macPath)
            compilerArgs.addAll("-x", "objective-c++")
            compilerArgs.addAll("-mmacosx-version-min=10.10")
            compilerArgs.addJavaFrameworks()
            source.from(
                file("src/main/objectiveCpp/DarkMode.mm")
            )
        }
    }
    binaries.whenElementFinalized(CppSharedLibrary::class) {
        linkTask.get().apply {
            dependsOn(macPath)
            linkerArgs.addAll("-lobjc", "-framework", "AppKit")
            linkerArgs.addJavaFrameworks()
        }
    }
}
