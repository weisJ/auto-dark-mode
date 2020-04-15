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

library {
    targetMachines.addAll(machines.windows.x86, machines.windows.x86_64)
    binaries.whenElementFinalized(CppSharedLibrary::class) {
        linkTask.get().linkerArgs.addAll(
            when (toolChain) {
                is Gcc, is Clang -> listOf("-luser32", "-ladvapi32")
                is VisualCpp -> listOf("user32.lib", "Advapi32.lib")
                else -> emptyList()
            }
        )
    }
}
