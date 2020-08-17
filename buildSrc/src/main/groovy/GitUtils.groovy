import groovy.transform.CompileStatic

@CompileStatic
class GitUtils {

    static String currentGitBranch() {
        def branch = ""
        def proc = "git rev-parse --abbrev-ref HEAD".execute()
        proc.in.eachLine { line -> branch = line }
        proc.err.eachLine { line -> println line }
        proc.waitFor()
        branch
    }
}
