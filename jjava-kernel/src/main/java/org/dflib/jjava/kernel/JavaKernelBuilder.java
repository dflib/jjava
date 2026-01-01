package org.dflib.jjava.kernel;

import jdk.jshell.JShell;
import org.dflib.jjava.jupyter.kernel.BaseKernelBuilder;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.JJavaExecutionControl;
import org.dflib.jjava.kernel.execution.JJavaLoaderDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A common builder superclass for JJavaKernel and subclasses.
 */
public abstract class JavaKernelBuilder<
        B extends JavaKernelBuilder<B, K>,
        K extends JavaKernel> extends BaseKernelBuilder<B, K> {

    protected long timeoutDuration;
    protected TimeUnit timeoutUnit;
    protected final List<String> compilerOpts;

    protected JavaKernelBuilder() {
        this.compilerOpts = new ArrayList<>();
    }

    public B compilerOpts(Iterable<String> opts) {
        opts.forEach(this.compilerOpts::add);
        return (B) this;
    }

    public B timeout(long timeoutDuration, TimeUnit timeoutUnit) {
        this.timeoutDuration = timeoutDuration;
        this.timeoutUnit = Objects.requireNonNull(timeoutUnit);
        return (B) this;
    }

    @Override
    public abstract K build();

    protected JShell buildJShell(CodeEvaluator evaluator) {
        return JShell.builder()
                .out(System.out)
                .err(System.err)
                .in(System.in)
                .executionEngine(evaluator.getExecControlProvider(), Map.of())
                .compilerOptions(compilerOpts.toArray(new String[0]))
                .build();
    }

    protected CodeEvaluator buildCodeEvaluator(String name) {
        long timeoutDuration = this.timeoutUnit != null ? this.timeoutDuration : -1;
        TimeUnit timeoutUnit = this.timeoutUnit != null ? this.timeoutUnit : TimeUnit.MILLISECONDS;
        return new CodeEvaluator(
                name,
                new JJavaExecutionControl(new JJavaLoaderDelegate(), timeoutDuration, timeoutUnit));
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
}
