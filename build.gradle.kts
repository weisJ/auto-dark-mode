import GitUtils.currentGitBranch
import com.github.autostyle.generic.DefaultCopyrightStyle
import com.github.autostyle.gradle.BaseFormatExtension
import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.autostyle")
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
    kotlin("jvm") apply false
}

val skipAutostyle by props()
val skipJavadoc by props()
val enableMavenLocal by props()
val enableGradleMetadata by props()

val String.v: String get() = rootProject.extra["$this.version"] as String

val buildVersion = "auto-dark-mode".v

fun BaseFormatExtension.license() {
    licenseHeader(File("${project.rootDir}/LICENSE").readText()) {
        copyrightStyle("bat", DefaultCopyrightStyle.REM)
        copyrightStyle("cmd", DefaultCopyrightStyle.REM)
    }
    trimTrailingWhitespace()
    endWithNewline()
}

fun BaseFormatExtension.configFilter(init: PatternFilterable.() -> Unit) {
    filter {
        // Autostyle does not support gitignore yet https://github.com/autostyle/autostyle/issues/13
        exclude("out/**")
        if (project == rootProject) {
            exclude("gradlew*", "gradle/**")
        } else {
            exclude("bin/**")
        }
        init()
    }
}

allprojects {
    group = "com.github.weisj"
    version = buildVersion

    repositories {
        if (enableMavenLocal) {
            mavenLocal()
        }
        mavenCentral()
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://www.jetbrains.bintray.com/intellij-third-party-dependencies") }
    }

    val githubAccessToken by props("")

    plugins.withType<UsePrebuiltBinariesWhenUnbuildablePlugin> {
        prebuildBinaries {
            prebuildLibrariesFolder = "pre-build-libraries"
            github {
                user = "weisj"
                repository = "auto-dark-mode"
                workflow = "libs.yml"
                accessToken = githubAccessToken
                branches = listOf(currentGitBranch(), "master").distinct()
                manualDownloadUrl =
                    "https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22+is%3Asuccess"
            }
        }
    }

    if (!skipAutostyle) {
        apply(plugin = "com.github.autostyle")
        autostyle {
            kotlinGradle {
                ktlint()
            }
            format("properties") {
                configFilter {
                    include("**/*.properties")
                    exclude("**/gradle.properties")
                }
                license()
            }
            format("configs") {
                configFilter {
                    include("**/*.sh", "**/*.bsh", "**/*.cmd", "**/*.bat")
                    include("**/*.xsd", "**/*.xsl", "**/*.xml")
                    exclude("*.eclipseformat.xml")
                }
                license()
            }
            format("markdown") {
                filter.include("**/*.md")
                endWithNewline()
            }
            cpp {
                trimTrailingWhitespace()
                endWithNewline()
                license()
                eclipse {
                    configFile("${project.rootDir}/cpp.eclipseformat.xml")
                }
            }
        }

        plugins.withType<JavaPlugin> {
            autostyle {
                java {
                    importOrder("java", "javax", "org", "com")
                    removeUnusedImports()
                    license()
                    eclipse {
                        configFile("${project.rootDir}/java.eclipseformat.xml")
                    }
                }
            }
        }
        plugins.withType<JavaBasePlugin> {
            autostyle {
                kotlin {
                    ktlint {
                        userData(mapOf("disabled_rules" to "no-wildcard-imports"))
                    }
                    license()
                }
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
