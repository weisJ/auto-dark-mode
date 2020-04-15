package com.github.weisj.darkmode.platform;

public final class JREInfo {

    public static final String jreArchitecture = System.getProperty("sun.arch.data.model");
    public static final boolean isX86;
    public static final boolean isX64;
    public static final String X86 = "32";
    public static final String X64 = "64";

    static {
        isX64 = X64.equals(jreArchitecture);
        isX86 = X86.equals(jreArchitecture);
    }
}
