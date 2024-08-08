import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.properties.dsl.props
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless")
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
    id("org.ajoberstar.grgit")
    kotlin("jvm") apply false
}

val skipSpotless by props()
val skipJavadoc by props()
val enableMavenLocal by props()
val enableGradleMetadata by props()

val String.v: String get() = rootProject.extra["$this.version"] as String

val buildVersion = "auto-dark-mode".v

val githubAccessToken by props("")
val currentBranch = System.getenv("GITHUB_HEAD_REF") ?: grgit.branch.current()?.name

allprojects {
    group = "com.github.weisj"
    version = buildVersion

    repositories {
        if (enableMavenLocal) {
            mavenLocal()
        }
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/snapshots") }
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    }

    plugins.withType<UsePrebuiltBinariesWhenUnbuildablePlugin> {
        prebuiltBinaries {
            prebuiltLibrariesFolder = "pre-build-libraries"
            github(
                user = "weisj",
                repository = "auto-dark-mode",
                workflow = "libs.yml"
            ) {
                failIfLibraryIsMissing = false
                val currentBranch = currentBranch
                branches = listOfNotNull(currentBranch, "master", "v$buildVersion", buildVersion)
                accessToken = githubAccessToken
                manualDownloadUrl =
                    "https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22+is%3Asuccess"
                timeout = 50000
            }
        }
    }

    if (!skipSpotless) {
        apply(plugin = "com.diffplug.spotless")
        spotless {
            val spotlessRatchet by props(default = true)
            if (spotlessRatchet) {
                ratchetFrom("origin/master")
            }
            kotlinGradle {
                ktlint("ktlint".v)
            }
            format("markdown") {
                target("*.md")
                endWithNewline()
                trimTrailingWhitespace()
            }
            format("svg") {
                target("**/*.svg")
                targetExclude("**/build/**/*.svg")
                endWithNewline()
                trimTrailingWhitespace()
                eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.XML)
            }
            plugins.withType<dev.nokee.platform.jni.internal.plugins.JniLibraryPlugin>().configureEach {
                cpp {
                    target("**/*.cpp", "**/*.h")
                    targetExclude("**/objcpp/**")
                    endWithNewline()
                    trimTrailingWhitespace()
                    eclipseCdt().configFile("${project.rootDir}/config/cpp.eclipseformat.xml")
                    licenseHeaderFile("${project.rootDir}/config/LICENSE_HEADER_JAVA.txt")
                }
            }
            plugins.withType<JavaPlugin>().configureEach {
                java {
                    importOrder("java", "javax", "org", "com")
                    removeUnusedImports()
                    endWithNewline()
                    trimTrailingWhitespace()
                    eclipse().configFile("${project.rootDir}/config/java.eclipseformat.xml")
                    licenseHeaderFile("${project.rootDir}/config/LICENSE_HEADER_JAVA.txt")
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

    if (!enableGradleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    val javaVersion = JavaVersion.VERSION_17

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
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
