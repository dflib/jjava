package org.dflib.jjava;

import jdk.jshell.JShell;
import org.dflib.jjava.execution.CodeEvaluator;
import org.dflib.jjava.execution.JJavaExecutionControlProvider;
import org.dflib.jjava.execution.JJavaJShellBuilder;
import org.dflib.jjava.execution.JJavaMagicTranspiler;
import org.dflib.jjava.jupyter.kernel.BaseKernelBuilder;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;
import org.dflib.jjava.jupyter.kernel.util.GlobFinder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A common builder superclass for JJavaKernel and subclasses.
 */
public abstract class JJavaKernelBuilder<
        B extends JJavaKernelBuilder<B, K>,
        K extends JJavaKernel> extends BaseKernelBuilder<B, K> {

    public static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator, Pattern.LITERAL);

    protected final String jShellExecControlID;
    protected JJavaExecutionControlProvider jShellExecControlProvider;

    protected JJavaKernelBuilder() {
        this.jShellExecControlID = UUID.randomUUID().toString();
    }

    public B jShellExecControlProvider(JJavaExecutionControlProvider jShellExecControlProvider) {
        this.jShellExecControlProvider = jShellExecControlProvider;
        return (B) this;
    }

    @Override
    public abstract K build();

    protected JShell buildJShell(JJavaExecutionControlProvider jShellExecControlProvider) {
        return JJavaJShellBuilder.builder()
                .addClasspathFromString(System.getenv(Env.JJAVA_CLASSPATH))
                .compilerOptsFromString(System.getenv(Env.JJAVA_COMPILER_OPTS))
                .timeoutFromString(System.getenv(Env.JJAVA_TIMEOUT))
                .stdout(System.out)
                .stderr(System.err)
                .stdin(System.in)
                .build(jShellExecControlProvider, jShellExecControlID);
    }

    protected CodeEvaluator buildCodeEvaluator(JShell jShell, JJavaExecutionControlProvider jShellExecControlProvider) {

        List<String> startupScripts = new ArrayList<>();

        String scriptPaths = System.getenv(Env.JJAVA_STARTUP_SCRIPTS_PATH);
        if (scriptPaths != null && !scriptPaths.isBlank()) {
            appendCodeFromScriptPaths(startupScripts, scriptPaths);
        }

        String code = System.getenv(Env.JJAVA_STARTUP_SCRIPT);
        if (code != null) {
            startupScripts.add(code);
        }

        return new CodeEvaluator(jShell, jShellExecControlProvider, jShellExecControlID, startupScripts);
    }

    private void appendCodeFromScriptPaths(List<String> startupScripts, String scriptPaths) {
        for (String glob : PATH_SPLITTER.split(scriptPaths)) {
            Iterable<Path> globScriptPaths;

            try {
                globScriptPaths = new GlobFinder(glob).computeMatchingPaths();
            } catch (IOException e) {
                throw new RuntimeException(String.format("Error while computing startup scripts for '%s': %s", glob, e.getMessage()), e);
            }

            for (Path path : globScriptPaths) {
                if (Files.isRegularFile(path) && Files.isReadable(path)) {
                    try {
                        startupScripts.add(Files.readString(path));
                    } catch (IOException e) {
                        throw new RuntimeException(String.format("Error while loading startup script for '%s': %s", path, e.getMessage()), e);
                    }
                }
            }
        }
    }

    protected boolean extensionsEnabled() {
        String envValue = System.getenv(Env.JJAVA_LOAD_EXTENSIONS);
        if (envValue == null) {
            return true;
        }
        String envValueTrimmed = envValue.trim();
        return !envValueTrimmed.isEmpty()
                && !envValueTrimmed.equals("0")
                && !envValueTrimmed.equalsIgnoreCase("false");
    }

    protected MagicParser buildMagicParser() {
        return magicParser != null
                ? magicParser
                : new MagicParser("(?<=(?:^|=))\\s*%", "%%", new JJavaMagicTranspiler());
    }

    protected LanguageInfo buildLanguageInfo() {
        return new LanguageInfo.Builder("Java")
                .version(Runtime.version().toString())
                .mimetype("text/x-java-source")
                .fileExtension(".jshell")
                .pygments("java")
                .codemirror("java")
                .build();
    }

    protected JJavaExecutionControlProvider buildJShellExecControlProvider(String name) {
        return this.jShellExecControlProvider != null
                ? jShellExecControlProvider
                : new JJavaExecutionControlProvider(name);
    }
}