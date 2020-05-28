import dev.nokee.platform.jni.JniLibraryExtension
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.stream.Stream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class UsePrebuiltBinariesWhenUnbuildablePlugin implements Plugin<Project> {

    private static final String TEMP_ZIP_PATH = "tmp${File.separator}prebuild${File.separator}tmp.zip"
    private static final String PRE_BUILD_PATH = "libs${File.separator}prebuild"
    private static final String GET_URL = "https://api.github.com/repos/weisj/auto-dark-mode/actions/workflows/libs.yml/runs"
    private String githubAccessToken

    @Override
    void apply(Project project) {
        githubAccessToken = getAccessToken(project)
        def library = project.extensions.getByType(JniLibraryExtension)
        library.variants.configureEach {
            if (!it.sharedLibrary.buildable) {
                // Try to include the library file... if available
                def downloadUrl = "https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22"
                def defaultLibraryName = project.property("defaultLibraryName")
                def variantName = JniUtils.asVariantName(it.targetMachine)
                def libraryFile = project.file("libraries/$variantName/$defaultLibraryName")
                def relativePath = project.rootProject.relativePath(libraryFile)
                if (!libraryFile.exists()) {
                    if (githubAccessToken == null) {
                        project.logger.error("""No github access token is specified. Latest artifacts will need to be included manually.
                                    |The access token needs to have the 'read-public' property. Specify using:
                                    |    -PgithubAccessToken=<your token>
                                    |or by setting
                                    |    githubAccessToken=<your token>
                                    |inside the gradle.properties file.
                                    |""".stripMargin())
                    }
                    getExternalBinary(project, variantName).ifPresentOrElse { file ->
                        project.logger.warn("${project.name}: Using pre-build library from github for targetMachine $variantName.")
                        it.nativeRuntimeFiles.setFrom(file)
                    } {
                        project.logger.error("""${project.name}: Library $relativePath for targetMachine $variantName does not exist or could not be downloaded.
                            |${(" " * (project.name.size() + 1))} Download it from $downloadUrl
                            |""".stripMargin())
                    }
                } else {
                    //Use provided library.
                    project.logger.warn("${project.name}: Using pre-build library $relativePath for targetMachine $variantName.")
                    it.nativeRuntimeFiles.setFrom(libraryFile)
                }
            }
        }
    }

    private Optional<File> getExternalBinary(Project project, String variant) {
        return getBinaryDownloadUrl(project, variant).map { getBinaryFromUrl(project, it).orElse(null) }
    }

    private Optional<File> getBinaryFromUrl(Project project, String url) {
        File directory = createDirectory(preBuildPath(project))
        return downloadZipFile(project, url).map { unzip(it, directory).findFirst() }.orElse(Optional.empty())
    }

    private static String preBuildPath(Project project) {
        return "${project.buildDir}${File.separator}$PRE_BUILD_PATH"
    }

    private Optional<ZipFile> downloadZipFile(Project project, String url) {
        HttpURLConnection conn = new URL(url).openConnection() as HttpURLConnection
        conn.setRequestMethod("GET")
        githubAccessToken?.with {
            conn.setRequestProperty("Authorization", "token $it")
        }
        conn.connect()
        def responseCode = conn.getResponseCode()
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try {
                File file = createFile(zipPath(project))
                Path response = file.toPath()
                Files.copy(conn.getInputStream(), response, StandardCopyOption.REPLACE_EXISTING)
                return Optional.of(new ZipFile(file))
            } catch (Exception ignored) {
                project.logger.warn("Could not download '$url'")
            }
        }
        return Optional.empty()
    }

    private static String zipPath(Project project) {
        return "$project.buildDir${File.separator}$TEMP_ZIP_PATH"
    }

    private static Stream<File> unzip(ZipFile self, File directory) {
        Collection<ZipEntry> files = self.entries().findAll { !(it as File).directory }
        return files.stream().map {
            ZipEntry e = it as ZipEntry
            e.name.with { fileName ->
                File outputFile = createFile("${directory.path}$File.separator$fileName")
                Files.copy(self.getInputStream(e), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                return outputFile
            }
        }
    }

    private Optional<String> getBinaryDownloadUrl(Project project, String artifactName) {
        return getArtifactsUrl(project).map { url ->
            Map[] artifacts = getJson(project, url).get("artifacts") as Map[]
            return artifacts?.find { artifactName == it.get("name") }?.get("url") as String
        }.map {
            getJson(project, it)?.get("archive_download_url") as String
        }
    }

    private Optional<String> getArtifactsUrl(Project project) {
        return Optional.ofNullable(getLatestRun(getJson(project, GET_URL)).with {
            project.logger.info("Using artifact from ${it.get("created_at")}")
            return get("artifacts_url") as String
        })
    }

    private Map getLatestRun(Map json) {
        Map[] runs = json.get("workflow_runs") as Map[]
        return Optional.ofNullable(runs.find { run ->
            boolean completed = "completed" == run.get("status")
            boolean success = "success" == run.get("conclusion")
            return completed && success
        }).orElse(Collections.emptyMap())
    }

    private Map getJson(Project project, String url) {
        HttpURLConnection get = new URL(url).openConnection() as HttpURLConnection
        get.setRequestMethod("GET")
        githubAccessToken?.with {
            get.setRequestProperty("Authorization", "token $it")
        }
        def responseCode = get.getResponseCode()
        if (responseCode == HttpURLConnection.HTTP_OK) {
            JsonSlurper jsonParser = new JsonSlurper()
            Map parsedJson = jsonParser.parseText(get.getInputStream().getText()) as Map
            return parsedJson
        } else {
            project.logger.warn("Could not fetch $url. Response code `$responseCode`.")
            return Collections.emptyMap()
        }
    }

    private static File createFile(String fileName) {
        File file = new File(fileName)
        if (file.exists()) file.delete()
        file.getParentFile().mkdirs()
        file.createNewFile()
        return file
    }

    private static File createDirectory(String fileName) {
        File file = new File(fileName)
        file.mkdirs()
        return file
    }

    private static String getAccessToken(Project project) {
        if (project.hasProperty("githubAccessToken")) {
            return project["githubAccessToken"]
        } else if (project.rootProject.hasProperty("githubAccessToken")) {
            return project.rootProject["githubAccessToken"]
        }
        return null
    }
}
