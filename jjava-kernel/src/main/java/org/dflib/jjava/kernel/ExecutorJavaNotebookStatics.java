package org.dflib.jjava.kernel;

/**
 * Kernel access for executor (user cell) code. Always fails; the managing JVM's kernel is never exposed to user code.
 */
public class ExecutorJavaNotebookStatics {

    private ExecutorJavaNotebookStatics() {
    }

    /**
     * @deprecated in favor of {@link #kernel()}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static JavaKernel getKernelInstance() {
        return kernel();
    }

    public static JavaKernel kernel() {
        throw new UnsupportedOperationException("JavaKernel.kernel() cannot expose the managing JVM from executor code");
    }
}
