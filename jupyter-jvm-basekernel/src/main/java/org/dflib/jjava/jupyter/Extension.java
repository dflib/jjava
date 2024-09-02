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
 * @since 1.0
 * @see BaseKernel
 */
public interface Extension {

    /**
     * Installs the extension into the given {@link BaseKernel}. This method is called by the
     * kernel during dependency loading. Implementations of this method should perform
     * any necessary setup or registration required by the extension to integrate with the kernel.
     *
     * @param kernel the {@link BaseKernel} instance into which the extension should be installed
     */
    void install(BaseKernel kernel);
}
