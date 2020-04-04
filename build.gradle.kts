import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.properties.dsl.props

plugins {
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
    id("com.github.vlsi.stage-vote-release")
    id("org.jetbrains.intellij")
}

val skipJavadoc by props()
val enableMavenLocal by props()
val enableGradleMetadata by props()

val String.v: String get() = rootProject.extra["$this.version"] as String

val buildVersion = "auto-dark-mode".v + releaseParams.snapshotSuffix

releaseParams {
    tlp.set("darklaf")
    organizationName.set("weisJ")
    componentName.set("darklaf")
    prefixForProperties.set("gh")
    svnDistEnabled.set(false)
    sitePreviewEnabled.set(false)
    nexus {
        mavenCentral()
    }
    voteText.set {
        """
        ${it.componentName} v${it.version}-rc${it.rc} is ready for preview.

        Git SHA: ${it.gitSha}
        Staging repository: ${it.nexusRepositoryUri}
        """.trimIndent()
    }
}

intellij {
    version = "2019.3.4"
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}

allprojects {
    group = "com.github.weisj"
    version = buildVersion

    repositories {
        if (enableMavenLocal) {
            mavenLocal()
        }
        mavenCentral()
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
            // cpp-library is not compatible with java-library
            // they both use api and implementation configurations
            val bom = platform(project(":auto-dark-mode-dependencies-bom"))
            if (!plugins.hasPlugin("cpp-library")) {
                "api"(bom)
            } else {
                // cpp-library does not know these configurations, so they are for Java
                "compileOnly"(bom)
                "runtimeOnly"(bom)
            }
        }
    }

    plugins.withId("cpp-library") {
        listOf(AbstractPublishToMaven::class, GenerateMavenPom::class, GenerateModuleMetadata::class)
                .forEach { type ->
                    tasks.withType(type)
                            .matching {
                                it.name.startsWith("publishMain") ||
                                        it.name.startsWith("signMain") ||
                                        it.name.startsWith("generatePomFileForMain") ||
                                        it.name.startsWith("generateMetadataFileForMain")
                            }
                            .configureEach {
                                // We don't need to publish CPP artifacts (e.g. header files)
                                enabled = false
                            }
                }
    }

    if (!enableGradleMetadata) {
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            withSourcesJar()
        }

        apply(plugin = "maven-publish")

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "MIT"
                    attributes["Implementation-Title"] = project.name
                    attributes["Implementation-Version"] = project.version
                    attributes["Specification-Vendor"] = "Darklaf"
                    attributes["Specification-Version"] = project.version
                    attributes["Specification-Title"] = "Darklaf"
                    attributes["Implementation-Vendor"] = "Darklaf"
                    attributes["Implementation-Vendor-Id"] = "com.github.weisj"
                }

                CrLfSpec(LineEndings.LF).run {
                    into("META-INF") {
                        filteringCharset = "UTF-8"
                        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        // This includes either project-specific license or a default one
                        if (file("$projectDir/LICENSE").exists()) {
                            textFrom("$projectDir/LICENSE")
                        } else {
                            textFrom("$rootDir/LICENSE")
                        }
                    }
                }
            }
        }

        configure<PublishingExtension> {
            if (project.path.startsWith(":auto-dark-mode-dependencies-bom") ||
                    project.path == ":") {
                // We don't it to Central for now
                return@configure
            }

            publications {
                create<MavenPublication>(project.name) {
                    artifactId = project.name
                    version = rootProject.version.toString()
                    description = project.description
                    from(project.components["java"])
                }
                withType<MavenPublication> {
                    // Use the resolved versions in pom.xml
                    // Gradle might have different resolution rules, so we set the versions
                    // that were used in Gradle build/test.
                    versionMapping {
                        usage(Usage.JAVA_RUNTIME) {
                            fromResolutionResult()
                        }
                        usage(Usage.JAVA_API) {
                            fromResolutionOf("runtimeClasspath")
                        }
                    }
                    pom {
                        withXml {
                            val sb = asString()
                            var s = sb.toString()
                            // <scope>compile</scope> is Maven default, so delete it
                            s = s.replace("<scope>compile</scope>", "")
                            // Cut <dependencyManagement> because all dependencies have the resolved versions
                            s = s.replace(
                                    Regex(
                                            "<dependencyManagement>.*?</dependencyManagement>",
                                            RegexOption.DOT_MATCHES_ALL
                                    ),
                                    ""
                            )
                            sb.setLength(0)
                            sb.append(s)
                            // Re-format the XML
                            asNode()
                        }


                        description.set(
                                project.description
                                        ?: "A themeable Look and Feel for java swing"
                        )
                        name.set(
                                (project.findProperty("artifact.name") as? String)
                                        ?: project.name.capitalize().replace("-", " ")
                        )
                        url.set("https://github.com/weisJ/darklaf")
                        organization {
                            name.set("com.github.weisj")
                            url.set("https://github.com/weisj")
                        }
                        issueManagement {
                            system.set("GitHub")
                            url.set("https://github.com/weisJ/darklaf/issues")
                        }
                        licenses {
                            license {
                                name.set("MIT")
                                url.set("https://github.com/weisJ/darklaf/blob/master/LICENSE")
                                distribution.set("repo")
                            }
                        }
                        scm {
                            url.set("https://github.com/weisJ/intelliJ-dark") //Todo
                            connection.set("scm:git:git://github.com/weisJ/darklaf.git")
                            developerConnection.set("scm:git:ssh://git@github.com:weisj/darklaf.git")
                        }
                        developers {
                            developer {
                                name.set("Jannis Weis")
                            }
                        }
                    }
                }
            }
        }
    }
}
