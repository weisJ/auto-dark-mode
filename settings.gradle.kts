enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    plugins {
        fun String.v() = extra["$this.version"].toString()
        fun PluginDependenciesSpec.idv(id: String, key: String = id) = id(id) version key.v()

        idv("com.diffplug.spotless")
        idv("com.github.vlsi.crlf", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.gradle-extensions", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.ide", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.license-gather", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.stage-vote-release", "com.github.vlsi.vlsi-release-plugins")
        idv("org.jetbrains.intellij", "org.jetbrains.intellij")
        idv("org.ajoberstar.grgit", "org.ajoberstar.grgit")
        idv("org.jetbrains.kotlin.jvm", "kotlin")
        idv("com.google.devtools.ksp")
    }
}

include(
    "base",
    "plugin",
    "windows",
    "macos",
    "linux",
    "linux/gtk"
)

rootProject.name = "auto-dark-mode"

for (p in rootProject.children) {
    if (p.children.isEmpty()) {
        // Rename leaf projects only
        // E.g. we don't expect to publish examples as a Maven module
        p.name = rootProject.name + "-" + p.name.replace("/", "-")
    }
}
