package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.util.Objects;

/**
 * An automatically-loaded extension that exposes a collection of static methods for notebook code to interact with the
 * kernel.
 */
public class JavaNotebookStatics implements Extension {

    static JavaKernel kernel;

    @Override
    public void install(BaseKernel kernel) {

        if (JavaNotebookStatics.kernel != null) {
            throw new IllegalStateException("Already initialized with a different kernel: " + JavaNotebookStatics.kernel.getBanner());
        }

        if (kernel instanceof JavaKernel) {
            JavaNotebookStatics.kernel = (JavaKernel) kernel;
        } else {
            // TODO: should we have some kind of abstract logger?
            System.err.println("Ignoring unexpected kernel '" + kernel.getClass().getName() + "'. Only '" + JavaKernel.class.getName() + "' is supported.");
        }
    }

    @Override
    public void uninstall(BaseKernel kernel) {

        if (JavaNotebookStatics.kernel != null && JavaNotebookStatics.kernel != kernel) {
            throw new IllegalStateException("Was initialized with a different kernel: " + JavaNotebookStatics.kernel.getBanner());
        }

        JavaNotebookStatics.kernel = null;
    }

    /**
     * @deprecated in favor of {@link #kernel()}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static JavaKernel getKernelInstance() {
        // TODO: should we have some kind of abstract logger?
        System.err.println("'getKernelInstance()' was deprecated in favor of 'kernel()' and will be eventually removed.");
        return kernel();
    }

    public static JavaKernel kernel() {
        return Objects.requireNonNull(
                JavaNotebookStatics.kernel,
                "No Java kernel running. Likely called outside of the notebook lifecycle");
    }
}
