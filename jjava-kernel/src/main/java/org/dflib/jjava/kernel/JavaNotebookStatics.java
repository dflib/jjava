package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.util.Objects;

/**
 * A collection of methods exposed in every notebook via static imports. For the methods to work, an instance of
 * JavaNotebookStatics must be loaded as a kernel extension, as this is when the kernel becomes known to the class.
 */
public class JavaNotebookStatics implements Extension {

    private static final String STARTUP_SCRIPT = "import static org.dflib.jjava.kernel.JavaNotebookStatics.*;";

    private static JavaKernel kernel;

    @Override
    public void install(BaseKernel kernel) {

        if (JavaNotebookStatics.kernel != null) {
            throw new IllegalStateException("Already initialized with another kernel: " + JavaNotebookStatics.kernel.getBanner());
        }

        if (kernel instanceof JavaKernel) {
            kernel.eval(STARTUP_SCRIPT);
            JavaNotebookStatics.kernel = (JavaKernel) kernel;
        } else {
            // TODO: should we have some kind of abstract logger?
            System.err.println("Ignoring unexpected kernel '" + kernel.getClass().getName() + "'. Only '" + JavaKernel.class.getName() + "' is supported.");
        }
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
