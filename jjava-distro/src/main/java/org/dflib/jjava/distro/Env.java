package org.dflib.jjava.distro;

import org.dflib.jjava.jupyter.kernel.util.GlobFinder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Defines environment variables supported by the kernel.
 */
final class Env {

    public static final String JJAVA_COMPILER_OPTS = "JJAVA_COMPILER_OPTS";
    public static final String JJAVA_TIMEOUT = "JJAVA_TIMEOUT";
    public static final String JJAVA_CLASSPATH = "JJAVA_CLASSPATH";
    public static final String JJAVA_STARTUP_SCRIPTS_PATH = "JJAVA_STARTUP_SCRIPTS_PATH";

    /**
     * A snippet of Java code to run when the kernel starts up.
     */
    public static final String JJAVA_STARTUP_SCRIPT = "JJAVA_STARTUP_SCRIPT";
    public static final String JJAVA_LOAD_EXTENSIONS = "JJAVA_LOAD_EXTENSIONS";

    // not used by JJava, but rather by the kernel launcher script
    public static final String JJAVA_JVM_OPTS = "JJAVA_JVM_OPTS";

    private static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator, Pattern.LITERAL);

    public static String timeout() {
        return System.getenv(Env.JJAVA_TIMEOUT);
    }

    public static boolean extensionsEnabled() {
        String envValue = System.getenv(Env.JJAVA_LOAD_EXTENSIONS);
        if (envValue == null) {
            return true;
        }

        String envValueTrimmed = envValue.trim();
        return !envValueTrimmed.isEmpty()
                && !envValueTrimmed.equals("0")
                && !envValueTrimmed.equalsIgnoreCase("false");
    }

    public static List<String> compilerOpts() {
        String optsString = System.getenv(Env.JJAVA_COMPILER_OPTS);
        return optsString != null ? Opts.splitOpts(optsString) : java.util.List.of();
    }

    public static List<String> extraClasspath() {
        String cpString = System.getenv(Env.JJAVA_CLASSPATH);
        return cpString != null ? java.util.List.of(PATH_SPLITTER.split(cpString)) : java.util.List.of();
    }

    public static List<String> startupSnippets() {
        List<String> snippets = new ArrayList<>();

        String scriptPaths = System.getenv(Env.JJAVA_STARTUP_SCRIPTS_PATH);
        if (scriptPaths != null && !scriptPaths.isBlank()) {
            appendSnippestFromScriptPaths(snippets, scriptPaths);
        }

        String code = System.getenv(Env.JJAVA_STARTUP_SCRIPT);
        if (code != null) {
            snippets.add(code);
        }

        return snippets;
    }

    private static void appendSnippestFromScriptPaths(List<String> startupScripts, String scriptPaths) {
        for (String glob : PATH_SPLITTER.split(scriptPaths)) {
            Iterable<Path> globScriptPaths;

            try {
                globScriptPaths = new GlobFinder(glob).computeMatchingPaths();
            } catch (IOException e) {
                throw new RuntimeException(java.lang.String.format("Error while computing startup scripts for '%s': %s", glob, e.getMessage()), e);
            }

            for (Path path : globScriptPaths) {
                if (Files.isRegularFile(path) && Files.isReadable(path)) {
                    try {
                        startupScripts.add(Files.readString(path));
                    } catch (IOException e) {
                        throw new RuntimeException(java.lang.String.format("Error while loading startup script for '%s': %s", path, e.getMessage()), e);
                    }
                }
            }
        }
    }
}
