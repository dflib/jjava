package org.dflib.jjava;

/**
 * Defines environment variables supported by the kernel.
 */
public final class Env {

    public static final String JJAVA_COMPILER_OPTS = "JJAVA_COMPILER_OPTS";
    public static final String JJAVA_TIMEOUT = "JJAVA_TIMEOUT";
    public static final String JJAVA_CLASSPATH = "JJAVA_CLASSPATH";
    public static final String JJAVA_STARTUP_SCRIPTS_PATH = "JJAVA_STARTUP_SCRIPTS_PATH";
    public static final String JJAVA_STARTUP_SCRIPT = "JJAVA_STARTUP_SCRIPT";
    public static final String JJAVA_LOAD_EXTENSIONS = "JJAVA_LOAD_EXTENSIONS";

    // not used by JJava, but rather by the kernel launcher script
    public static final String JJAVA_JVM_OPTS = "JJAVA_JVM_OPTS";
}
