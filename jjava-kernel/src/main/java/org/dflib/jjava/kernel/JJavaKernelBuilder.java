package org.dflib.jjava.kernel;

import jdk.jshell.JShell;
import org.dflib.jjava.jupyter.kernel.BaseKernelBuilder;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.util.GlobFinder;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.JJavaExecutionControlProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A common builder superclass for JJavaKernel and subclasses.
 */
public abstract class JJavaKernelBuilder<
        B extends JJavaKernelBuilder<B, K>,
        K extends JJavaKernel> extends BaseKernelBuilder<B, K> {

    protected final String jShellExecControlID;
    protected JJavaExecutionControlProvider jShellExecControlProvider;
    protected String timeout;
    protected final List<String> startupSnippets;
    protected final List<String> compilerOpts;
    protected final List<String> extraClasspath;

    protected JJavaKernelBuilder() {
        this.jShellExecControlID = UUID.randomUUID().toString();
        this.startupSnippets = new ArrayList<>();
        this.compilerOpts = new ArrayList<>();
        this.extraClasspath = new ArrayList<>();
    }

    public B jShellExecControlProvider(JJavaExecutionControlProvider jShellExecControlProvider) {
        this.jShellExecControlProvider = jShellExecControlProvider;
        return (B) this;
    }

    /**
     * Adds a snippet of script code to execute on startup.
     */
    public B startupSnippets(Iterable<String> code) {
        code.forEach(startupSnippets::add);
        return (B) this;
    }

    public B compilerOpts(Iterable<String> opts) {
        opts.forEach(this.compilerOpts::add);
        return (B) this;
    }

    public B extraClasspath(Iterable<String> cp) {
        cp.forEach(this.extraClasspath::add);
        return (B) this;
    }

    /**
     * Sets the kernel communication timeout. The String should be a number with a {@link java.util.concurrent.TimeUnit}
     * name. E.g. "30 SECONDS"
     */
    public B timeout(String timeout) {
        this.timeout = timeout;
        return (B) this;
    }

    @Override
    public abstract K build();

    protected JShell buildJShell(JJavaExecutionControlProvider jShellExecControlProvider) {

        Map<String, String> execControlParams = new HashMap<>();
        execControlParams.put(JJavaExecutionControlProvider.REGISTRATION_ID_KEY, jShellExecControlID);

        if (timeout != null) {
            execControlParams.put(JJavaExecutionControlProvider.TIMEOUT_KEY, timeout);
        }

        JShell shell = JShell.builder()
                .out(System.out)
                .err(System.err)
                .in(System.in)
                .executionEngine(jShellExecControlProvider, execControlParams)
                .compilerOptions(compilerOpts.toArray(new String[0]))
                .build();

        for (String cp : extraClasspath) {

            if (cp.isBlank()) {
                continue;
            }

            Iterable<Path> paths;
            try {
                paths = new GlobFinder(cp).computeMatchingPaths();
            } catch (IOException e) {
                throw new RuntimeException(String.format("Error computing classpath entries for '%s': %s", cp, e.getMessage()), e);
            }

            for (Path entry : paths) {
                // TODO: not loading JJava extensions from classpath here.
                //  Is this the cause of https://github.com/dflib/jjava/issues/71 ?
                shell.addToClasspath(entry.toAbsolutePath().toString());
            }
        }

        return shell;
    }

    protected CodeEvaluator buildCodeEvaluator(JShell jShell, JJavaExecutionControlProvider jShellExecControlProvider) {
        return new CodeEvaluator(jShell, jShellExecControlProvider, jShellExecControlID, startupSnippets);
    }

    protected MagicParser buildMagicParser(MagicTranspiler transpiler) {
        return magicParser != null
                ? magicParser
                : new MagicParser("(?<=(?:^|=))\\s*%", "%%", transpiler);
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