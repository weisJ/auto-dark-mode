import dev.nokee.platform.jni.JniLibraryExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class UsePrebuiltBinariesWhenUnbuildablePlugin implements Plugin<Project> {

    private static final String DOWNLOAD_URL = "https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22+is%3Asuccess"
    private boolean alwaysUsePrebuildArtifact = false

    @Override
    void apply(Project project) {
        def library = project.extensions.getByType(JniLibraryExtension)
        library.variants.configureEach { var ->
            if (alwaysUsePrebuildArtifact || !var.sharedLibrary.buildable) {
                // Try to include the library file... if available
                def defaultLibraryName = JniUtils.getLibraryFileNameFor(project, var.targetMachine.operatingSystemFamily)
                def variantName = JniUtils.asVariantName(var.targetMachine)
                def libraryFile = project.file("libraries/$variantName/$defaultLibraryName")

                if (!libraryFile.exists()) {
                    // No local binary provided. Try to download it from github actions.
                    def prebuiltBinariesTask = project.tasks.register("downloadPrebuiltBinary$variantName", DownloadPrebuiltBinaryFromGitHubAction.class)
                    prebuiltBinariesTask.configure {
                        it.githubAccessToken = getAccessToken(project)
                        it.variant = variantName
                        it.user = "weisj"
                        it.repository = "auto-dark-mode"
                        it.workflow = "libs.yml"
                        it.manualDownlaodUrl = DOWNLOAD_URL
                    }
                    var.nativeRuntimeFiles.setFrom(prebuiltBinariesTask.map { it.prebuiltBinaryFile })
                    var.nativeRuntimeFiles.from(new CallableLogger({
                        project.logger.warn("${project.name}: Using pre-build library from github for targetMachine $variantName.")
                    }))
                } else {
                    //Use provided library.
                    var.nativeRuntimeFiles.setFrom(libraryFile)
                    var.nativeRuntimeFiles.from(new CallableLogger({
                        def relativePath = project.rootProject.relativePath(libraryFile)
                        project.logger.warn("${project.name}: Using pre-build library $relativePath for targetMachine $variantName.")
                    }))
                }
            }
        }
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
