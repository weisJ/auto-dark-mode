import dev.nokee.platform.jni.JniJarBinary
import dev.nokee.platform.jni.JniLibraryExtension
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
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
        library.binaries.withType(JniJarBinary).configureEach {
            if (it.jarTask.isPresent()) it.jarTask.get()?.enabled = false
        }
        if (library.targetMachines.get().size() >= 1) {
            logger.info("${project.name}: Merging binaries into the JVM Jar.")
            library.variants.configureEach {
                task.into(it.resourcePath) { CopySpec spec ->
                    spec.from(it.nativeRuntimeFiles)
                }
            }
        }
    }
}
