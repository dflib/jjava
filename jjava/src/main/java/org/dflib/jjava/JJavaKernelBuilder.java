package org.dflib.jjava;

import jdk.jshell.JShell;
import org.dflib.jjava.execution.CodeEvaluator;
import org.dflib.jjava.execution.JJavaExecutionControlProvider;
import org.dflib.jjava.execution.JJavaJShellBuilder;
import org.dflib.jjava.execution.JJavaMagicTranspiler;
import org.dflib.jjava.jupyter.kernel.BaseKernelBuilder;
import org.dflib.jjava.jupyter.kernel.LanguageInfo;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;

import java.util.UUID;

/**
 * A common builder superclass for JJavaKernel and subclasses.
 */
public abstract class JJavaKernelBuilder<
        B extends JJavaKernelBuilder<B, K>,
        K extends JJavaKernel> extends BaseKernelBuilder<B, K> {

    protected final String execControlID;
    protected JJavaExecutionControlProvider jShellExecControlProvider;

    protected JJavaKernelBuilder() {
        this.execControlID = UUID.randomUUID().toString();
    }

    public B jShellExecControlProvider(JJavaExecutionControlProvider jShellExecControlProvider) {
        this.jShellExecControlProvider = jShellExecControlProvider;
        return (B) this;
    }

    @Override
    public abstract K build();

    protected JShell buildJShell(JJavaExecutionControlProvider execControlProvider) {
        return JJavaJShellBuilder.builder()
                .addClasspathFromString(System.getenv(Env.JJAVA_CLASSPATH))
                .compilerOptsFromString(System.getenv(Env.JJAVA_COMPILER_OPTS))
                .timeoutFromString(System.getenv(Env.JJAVA_TIMEOUT))
                .stdout(System.out)
                .stderr(System.err)
                .stdin(System.in)
                .build(execControlProvider, execControlID);
    }

    protected CodeEvaluator buildCodeEvaluator(JShell jShell, JJavaExecutionControlProvider execControlProvider) {
        return CodeEvaluator.builder()
                .startupScriptFiles(System.getenv(Env.JJAVA_STARTUP_SCRIPTS_PATH))
                .startupScript(System.getenv(Env.JJAVA_STARTUP_SCRIPT))
                .build(jShell, execControlProvider, execControlID);
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