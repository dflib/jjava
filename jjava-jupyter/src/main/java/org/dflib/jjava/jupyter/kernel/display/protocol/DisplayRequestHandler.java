package org.dflib.jjava.jupyter.kernel.display.protocol;

/**
 * Manager-side boundary: applies one decoded display request to the notebook. Any exception it throws is reported to
 * the executor as a rejection.
 */
public interface DisplayRequestHandler {

    void handle(DisplayRequest request);
}
