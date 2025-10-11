package org.dflib.jjava.jupyter;

import org.dflib.jjava.jupyter.kernel.BaseKernel;

/**
 * This interface defines an extension loading service that can be implemented and registered in
 * {@code META-INF/services} to be automatically discovered and loaded by the kernel using a service loader mechanism.
 * <br>
 * Implementations of this interface are intended to bootstrap the extensions they represent.
 * <br>
 * To create an extension loader, implement this interface and provide the fully qualified class name
 * of your implementation in a file named {@code META-INF/services/org.dflib.jjava.jupyter.Extension}.
 *
 * @see BaseKernel
 */
public interface Extension {

    /**
     * A callback invoked by the kernel when the extension is loaded, allowing it to initialize its state.
     *
     * @param kernel the {@link BaseKernel} instance into which the extension should be installed
     */
    void install(BaseKernel kernel);

    /**
     * A callback invoked by the kernel on shutdown that allows the extension to clean up after itself.
     *
     * @param kernel the {@link BaseKernel} instance into which the extension was previously installed
     */
    default void uninstall(BaseKernel kernel) {
        // do nothing by default
    }
}
