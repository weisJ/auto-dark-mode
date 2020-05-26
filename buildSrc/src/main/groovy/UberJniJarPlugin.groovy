import dev.nokee.platform.jni.JniJarBinary
import dev.nokee.platform.jni.JniLibrary
import dev.nokee.platform.jni.JniLibraryExtension
import dev.nokee.platform.nativebase.TargetMachine
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.jvm.tasks.Jar

@CompileStatic
class UberJniJarPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.tasks.named('jar', Jar) { task ->
            configure(task)
        }
    }

    private static void configure(Jar task) {
        def project = task.getProject()
        def logger = task.getLogger()
        def library = project.extensions.getByType(JniLibraryExtension)
        def buildableVariants = library.variants.flatMap(onlyBuildableVariant())
        includeBuiltJniJarContent(logger, project, task, library, buildableVariants);

        def unbuildableVariants = library.variants.flatMap(onlyUnbuildableVariant())
        usePrebuiltBinaryIfAvailable(project, library, logger, task, unbuildableVariants);
    }

    private static void includeBuiltJniJarContent(Logger logger, Project project, Jar task, JniLibraryExtension library, Provider<List<? extends JniLibrary>> variants) {
        logger.info("${project.name}: Merging binaries into the JVM Jar.")
        // There is no need to specify the destination of the content as it's already configured via library.resourcePath property
        task.from(variants.map(jniLibraryBinaryFiles(project)))
    }

    private static Closure<List<Provider<FileTree>>> jniLibraryBinaryFiles(Project project) {
        return { List<? extends JniLibrary> variants ->
            // Single variant are collapse into the JVM Jar
            if (variants.size() > 1) {
                List<JniJarBinary> jniJarBinaries = []
                for (JniLibrary variant : variants) {
                    variant.binaries.withType(JniJarBinary).elements.get().each { jniJarBinaries << it }
                }

                List<Provider<FileTree>> result = []
                for (Provider<Jar> jarTask : jniJarBinaries*.jarTask) {
                    result << jarTask.map { project.zipTree(it.archiveFile).matching { PatternFilterable p -> p.exclude('META-INF/**/*') } }
                }
                return result
            }
            return [] as List<Provider<FileTree>>
        }
    }

    private static void usePrebuiltBinaryIfAvailable(Project project, JniLibraryExtension library, Logger logger, Jar task, Provider<List<? extends JniLibrary>> variants) {
        for (TargetMachine targetMachine : library.targetMachines.get()) {
            def libraryPath = "com/github/weisj/darkmode/${project.name}"
            def variantName = asVariantName(targetMachine)
            task.into("$libraryPath/$variantName") { CopySpec spec ->
                spec.from(variants.map(includeIfUnbuildable(project, logger, targetMachine)))
            }
        }
    }

    private static Closure<List<File>> includeIfUnbuildable(Project project, Logger logger, TargetMachine targetMachine) {
        boolean messageAlreadyLogged = false
        return { List<JniLibrary> variants ->
            // Check the unbuildable variant list for a matching target machine
            if (variants.find {it.targetMachine == targetMachine} != null) {
                // Try to include the library file... if available
                def downloadUrl = 'https://github.com/weisJ/auto-dark-mode/actions?query=workflow%3A%22Build+Native+Libraries%22'
                def defaultLibraryName = project.property('defaultLibraryName')
                def variantName = asVariantName(targetMachine)
                def libraryFile = project.file("libraries/$variantName/$defaultLibraryName")
                def relativePath = project.rootProject.relativePath(libraryFile)
                if (!messageAlreadyLogged) {
                    if (!libraryFile.exists()) {
                        logger.warn("""${project.name}: Library $relativePath for targetMachine $variantName does not exist.
                            |${" ".multiply(project.name.size() + 1)} Download it from $downloadUrl
                            |""".stripMargin())
                    } else {
                        //Use provided library.
                        logger.warn("${project.name}: Using pre-build library $relativePath for targetMachine $variantName.")
                    }
                    messageAlreadyLogged = true
                }
                if (libraryFile.exists()) {
                    return [libraryFile]
                }
            }
            return [] as List<File>
        }
    }

    static String asVariantName(TargetMachine targetMachine) {
        String operatingSystemFamily = 'macos'
        if (targetMachine.operatingSystemFamily.windows) {
            operatingSystemFamily = 'windows'
        }

        String architecture = 'x86-64'
        if (targetMachine.architecture.'32Bit') {
            architecture = 'x86'
        }

        return "$operatingSystemFamily-$architecture"
    }

    static Transformer<Iterable<JniLibrary>, JniLibrary> onlyBuildableVariant() {
        return new Transformer<Iterable<JniLibrary>, JniLibrary>() {
            @Override
            List<JniLibrary> transform(JniLibrary it) {
                if (it.sharedLibrary.buildable) {
                    return [it]
                }
                return []
            }
        }
    }

    static Transformer<Iterable<JniLibrary>, JniLibrary> onlyUnbuildableVariant() {
        return new Transformer<Iterable<JniLibrary>, JniLibrary>() {
            @Override
            List<JniLibrary> transform(JniLibrary it) {
                if (!it.sharedLibrary.buildable) {
                    return [it]
                }
                return []
            }
        }
    }
}
