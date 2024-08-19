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
    public static final String JJAVA_BOOTSTRAP_OFF = "JJAVA_BOOTSTRAP_OFF";

    // not used by Java, but rather by the Python kernel boot script
    public static final String JJAVA_JVM_OPTS = "JJAVA_JVM_OPTS";

}
