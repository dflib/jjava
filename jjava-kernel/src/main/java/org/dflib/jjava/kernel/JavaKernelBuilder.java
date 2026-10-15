package org.dflib.jjava.kernel;

import jdk.jshell.JShell;
import jdk.jshell.execution.JdiExecutionControlProvider;
import org.dflib.jjava.jupyter.kernel.BaseKernelBuilder;
import org.dflib.jjava.jupyter.kernel.display.protocol.DisplayRequestHandler;
import org.dflib.jjava.jupyter.kernel.JupyterIO;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.MagicsResolver;
import org.dflib.jjava.kernel.execution.CodeEvaluator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A common builder superclass for JJavaKernel and subclasses.
 */
@SuppressWarnings("unchecked")
public abstract class JavaKernelBuilder<
        B extends JavaKernelBuilder<B, K>,
        K extends JavaKernel> extends BaseKernelBuilder<B, K> {

    protected long timeoutDuration;
    protected TimeUnit timeoutUnit;
    protected final List<String> compilerOpts;
    protected final List<String> remoteVMOptions;

    // test seams: override the display endpoint address, and observe or fail the build after the endpoint exists
    Path displayEndpointPath;
    Runnable afterDisplayEndpointCreated = () -> {
    };
    DisplayRequestHandler displayHandler;

    protected JavaKernelBuilder() {
        this.compilerOpts = new ArrayList<>();
        this.remoteVMOptions = new ArrayList<>(List.of("-classpath", System.getProperty("java.class.path")));
    }

    public B compilerOpts(Iterable<String> opts) {
        opts.forEach(this.compilerOpts::add);
        return (B) this;
    }

    public B remoteVMOptions(Iterable<String> opts) {
        opts.forEach(this.remoteVMOptions::add);
        return (B) this;
    }

    B displayEndpointPath(Path path) {
        this.displayEndpointPath = path;
        return (B) this;
    }

    B afterDisplayEndpointCreated(Runnable hook) {
        this.afterDisplayEndpointCreated = hook;
        return (B) this;
    }

    B displayHandler(DisplayRequestHandler handler) {
        this.displayHandler = handler;
        return (B) this;
    }

    public B timeout(long timeoutDuration, TimeUnit timeoutUnit) {
        this.timeoutDuration = timeoutDuration;
        this.timeoutUnit = Objects.requireNonNull(timeoutUnit);
        return (B) this;
    }

    @Override
    public abstract K build();

    protected JShell buildJShell(JupyterIO io, Iterable<String> additionalRemoteVMOptions) {
        List<String> options = new ArrayList<>(remoteVMOptions);
        additionalRemoteVMOptions.forEach(options::add);
        return JShell.builder()
                .out(io.out)
                .err(io.err)
                .in(io.in)
                .executionEngine(new JdiExecutionControlProvider(), Map.of())
                .remoteVMOptions(options.toArray(new String[0]))
                .compilerOptions(compilerOpts.toArray(new String[0]))
                .build();
    }

    protected CodeEvaluator buildCodeEvaluator() {
        long timeoutDuration = this.timeoutUnit != null ? this.timeoutDuration : -1;
        TimeUnit timeoutUnit = this.timeoutUnit != null ? this.timeoutUnit : TimeUnit.MILLISECONDS;
        return new CodeEvaluator(timeoutDuration, timeoutUnit);
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
