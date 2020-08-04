plugins {
    `java-library`
}

dependencies {
    /*
     * It's not necessary to add the base module as an implementation as the plugin already does this.
     * It is merely needed for compilation. The plugin module already has the base module as an implementation
     * so the classes will be available at runtime.
     */
    compileOnly(project(":auto-dark-mode-base"))
    /*
     * Add additional Linux submodules below as implementations. By doing this, all submodules
     * will be pulled into plugin/build.gradle.kts when it specifies implementation(project(":auto-dark-mode-linux")).
     */
    implementation(project(":auto-dark-mode-linux-gnome"))
}
