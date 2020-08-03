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
                compilerArgs.addAll(toolChain.map {
                    when (it) {
                        // These parameters (excluding --std=c++11) were obtained with `pkg-config --cflags glibmm-2.4 giomm-2.4 sigc++-2.0 gtk+-3.0`.
                        is Gcc, is Clang -> listOf(
                            "--std=c++11",
                            "-pthread",
                            "-I/usr/include/giomm-2.4",
                            "-I/usr/lib/x86_64-linux-gnu/giomm-2.4/include",
                            "-I/usr/include/glibmm-2.4",
                            "-I/usr/lib/x86_64-linux-gnu/glibmm-2.4/include",
                            "-I/usr/include/sigc++-2.0",
                            "-I/usr/lib/x86_64-linux-gnu/sigc++-2.0/include",
                            "-I/usr/include/gtk-3.0",
                            "-I/usr/include/at-spi2-atk/2.0",
                            "-I/usr/include/at-spi-2.0",
                            "-I/usr/include/dbus-1.0",
                            "-I/usr/lib/x86_64-linux-gnu/dbus-1.0/include",
                            "-I/usr/include/gtk-3.0",
                            "-I/usr/include/gio-unix-2.0",
                            "-I/usr/include/cairo",
                            "-I/usr/include/pango-1.0",
                            "-I/usr/include/fribidi",
                            "-I/usr/include/harfbuzz",
                            "-I/usr/include/atk-1.0",
                            "-I/usr/include/cairo",
                            "-I/usr/include/pixman-1",
                            "-I/usr/include/uuid",
                            "-I/usr/include/freetype2",
                            "-I/usr/include/libpng16",
                            "-I/usr/include/gdk-pixbuf-2.0",
                            "-I/usr/include/libmount",
                            "-I/usr/include/blkid",
                            "-I/usr/include/glib-2.0",
                            "-I/usr/lib/x86_64-linux-gnu/glib-2.0/include"
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
                        // These parameters were obtained with `pkg-config --libs glibmm-2.4 giomm-2.4 sigc++-2.0 gtk+-3.0`.
                        is Gcc, is Clang -> listOf(
                            "-lgiomm-2.4",
                            "-lglibmm-2.4",
                            "-lsigc-2.0",
                            "-lgtk-3",
                            "-lgdk-3",
                            "-lpangocairo-1.0",
                            "-lpango-1.0",
                            "-lharfbuzz",
                            "-latk-1.0",
                            "-lcairo-gobject",
                            "-lcairo",
                            "-lgdk_pixbuf-2.0",
                            "-lgio-2.0",
                            "-lgobject-2.0",
                            "-lglib-2.0"
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
