package org.dflib.jjava.jupyter.kernel.display.protocol;

/**
 * Executor-side boundary: hands one display request to the manager and returns only on confirmed delivery, otherwise
 * throws a {@link DisplayDeliveryException}.
 */
public interface DisplayDelivery {

    void deliver(DisplayRequest request);
}
