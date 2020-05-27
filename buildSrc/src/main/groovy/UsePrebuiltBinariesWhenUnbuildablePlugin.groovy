import dev.nokee.platform.jni.JniLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class UsePrebuiltBinariesWhenUnbuildablePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def library = project.extensions.getByType(JniLibraryExtension)
        library.variants.configureEach {
            if (!it.sharedLibrary.buildable) {
                // Try to include the library file... if available
                def downloadUrl = 'https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22'
                def defaultLibraryName = project.property('defaultLibraryName')
                def variantName = JniUtils.asVariantName(it.targetMachine)
                def libraryFile = project.file("libraries/$variantName/$defaultLibraryName")
                def relativePath = project.rootProject.relativePath(libraryFile)
                if (!libraryFile.exists()) {
                    project.logger.warn("""${project.name}: Library $relativePath for targetMachine $variantName does not exist.
                            |${" ".multiply(project.name.size() + 1)} Download it from $downloadUrl
                            |""".stripMargin())
                } else {
                    //Use provided library.
                    project.logger.warn("${project.name}: Using pre-build library $relativePath for targetMachine $variantName.")
                    it.nativeRuntimeFiles.setFrom(libraryFile)
                }
            }
        }
    }
}
