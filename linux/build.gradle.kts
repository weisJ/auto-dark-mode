import JniUtils.asVariantName

plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
}

library {
    dependencies {
        jvmImplementation(project(":auto-dark-mode-base"))
        jvmImplementation("com.github.weisj:darklaf-native-utils")
    }
    targetMachines.addAll(machines.linux.x86_64)
    variants.configureEach {
        /*
         * TODO: This build script assumes that Gnome is the only Linux desktop environment.
         *  The auto-dark-mode-linux module may need to be split into different subprojects
         *  such that there can be separate .so files for the different environments.
         */
        resourcePath.set("com/github/weisj/darkmode/${project.name}/${asVariantName(targetMachine)}")
        sharedLibrary {
            compileTasks.configureEach {
                compilerArgs.addAll(toolChain.map {
                    when (it) {
                        // These parameters (excluding --std=c++11) were obtained with `pkg-config --cflags glibmm-2.4 giomm-2.4 sigc++-2.0`.
                        is Gcc, is Clang -> listOf(
                            "--std=c++11",
                            "-pthread",
                            "-I/usr/include/giomm-2.4",
                            "-I/usr/lib/x86_64-linux-gnu/giomm-2.4/include",
                            "-I/usr/include/libmount",
                            "-I/usr/include/blkid",
                            "-I/usr/include/glibmm-2.4",
                            "-I/usr/lib/x86_64-linux-gnu/glibmm-2.4/include",
                            "-I/usr/include/glib-2.0",
                            "-I/usr/lib/x86_64-linux-gnu/glib-2.0/include",
                            "-I/usr/include/sigc++-2.0",
                            "-I/usr/lib/x86_64-linux-gnu/sigc++-2.0/include"
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
                        // These parameters were obtained with `pkg-config --libs glibmm-2.4 giomm-2.4 sigc++-2.0`.
                        is Gcc, is Clang -> listOf(
                            "-lgiomm-2.4",
                            "-lgio-2.0",
                            "-lglibmm-2.4",
                            "-lgobject-2.0",
                            "-lglib-2.0",
                            "-lsigc-2.0"
                        )
                        else -> emptyList()
                    }
                })
            }
        }
    }
}
