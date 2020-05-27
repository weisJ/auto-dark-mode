import dev.nokee.platform.nativebase.TargetMachine

class JniUtils {
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
}
