package org.dflib.jjava.kernel;

import jdk.jshell.JShell;
import org.dflib.jjava.jupyter.kernel.BaseKernelBuilder;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.JJavaExecutionControlProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A common builder superclass for JJavaKernel and subclasses.
 */
public abstract class JavaKernelBuilder<
        B extends JavaKernelBuilder<B, K>,
        K extends JavaKernel> extends BaseKernelBuilder<B, K> {

    protected final String jShellExecControlID;
    protected JJavaExecutionControlProvider jShellExecControlProvider;
    protected String timeout;
    protected final List<String> compilerOpts;

    protected JavaKernelBuilder() {
        this.jShellExecControlID = UUID.randomUUID().toString();
        this.compilerOpts = new ArrayList<>();
    }

    public B jShellExecControlProvider(JJavaExecutionControlProvider jShellExecControlProvider) {
        this.jShellExecControlProvider = jShellExecControlProvider;
        return (B) this;
    }

    public B compilerOpts(Iterable<String> opts) {
        opts.forEach(this.compilerOpts::add);
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

        return JShell.builder()
                .out(System.out)
                .err(System.err)
                .in(System.in)
                .executionEngine(jShellExecControlProvider, execControlParams)
                .compilerOptions(compilerOpts.toArray(new String[0]))
                .build();
    }

    protected CodeEvaluator buildCodeEvaluator(JShell jShell, JJavaExecutionControlProvider jShellExecControlProvider) {
        return new CodeEvaluator(jShell, jShellExecControlProvider, jShellExecControlID);
    }

    protected MagicsResolver buildMagicsResolver(MagicTranspiler transpiler) {
        return magicsResolver != null
                ? magicsResolver
                : new MagicsResolver("(?<=(?:^|=))\\s*%", "%%", transpiler);
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