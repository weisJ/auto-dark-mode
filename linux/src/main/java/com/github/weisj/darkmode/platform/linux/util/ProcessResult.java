package com.github.weisj.darkmode.platform.linux.util;

public class ProcessResult {
    private final int exitCode;
    private final String stdOut;
    private final String stdErr;

    public ProcessResult(int exitCode, String stdOut, String stdErr) {
        this.exitCode = exitCode;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdOut() {
        return stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }
}
