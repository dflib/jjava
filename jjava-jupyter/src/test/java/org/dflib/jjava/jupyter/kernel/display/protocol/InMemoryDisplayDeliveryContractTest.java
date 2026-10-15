package org.dflib.jjava.jupyter.kernel.display.protocol;

/**
 * Runs the delivery contract against an in-memory substitute that round-trips every request and response through the
 * protocol text, exactly as a real transport would.
 */
public class InMemoryDisplayDeliveryContractTest extends DisplayDeliveryContractTest {

    @Override
    protected DisplayDelivery delivery(DisplayRequestHandler handler) {
        return request -> {
            DisplayRequest received = DisplayProtocol.decode(DisplayProtocol.encode(request));
            String response;
            try {
                handler.handle(received);
                response = DisplayProtocol.response(null);
            } catch (RuntimeException e) {
                response = DisplayProtocol.response(e);
            }
            DisplayProtocol.checkResponse(response);
        };
    }
}
