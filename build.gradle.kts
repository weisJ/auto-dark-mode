import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
    kotlin("jvm") apply false
}

val skipJavadoc by props()
val enableMavenLocal by props()
val enableGradleMetadata by props()

val String.v: String get() = rootProject.extra["$this.version"] as String

val buildVersion = "auto-dark-mode".v

allprojects {
    group = "com.github.weisj"
    version = buildVersion

    repositories {
        if (enableMavenLocal) {
            mavenLocal()
        }
        mavenCentral()
    }

    val githubAccessToken by props("")

    plugins.withType<UsePrebuiltBinariesWhenUnbuildablePlugin>() {
        prebuildBinaries {
            prebuildLibrariesFolder = "pre-build-libraries"
            github {
                user = "weisj"
                repository = "auto-dark-mode"
                workflow = "libs.yml"
                accessToken = githubAccessToken
                manualDownloadUrl =
                    "https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22+is%3Asuccess"
            }
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        // Ensure builds are reproducible
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirMode = "775".toInt(8)
        fileMode = "664".toInt(8)
    }

    plugins.withType<JavaLibraryPlugin> {
        dependencies {
            "api"(platform(project(":auto-dark-mode-dependencies-bom")))
        }
    }

    if (!enableGradleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjvm-default=compatibility")
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            withSourcesJar()
        }

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "MIT"
                    attributes["Implementation-Title"] = project.name
                    attributes["Implementation-Version"] = project.version
                    attributes["Specification-Vendor"] = "Auto Dark Mode"
                    attributes["Specification-Version"] = project.version
                    attributes["Specification-Title"] = "Auto Dark Mode"
                    attributes["Implementation-Vendor"] = "Auto Dark Mode"
                    attributes["Implementation-Vendor-Id"] = "com.github.weisj"
                }

                CrLfSpec(LineEndings.LF).run {
                    into("META-INF") {
                        filteringCharset = "UTF-8"
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        // This includes either project-specific license, or a default one
                        if (file("$projectDir/LICENSE").exists()) {
                            textFrom("$projectDir/LICENSE")
                        } else {
                            textFrom("$rootDir/LICENSE")
                        }
                    }
                }
            }
        }
    }
}
