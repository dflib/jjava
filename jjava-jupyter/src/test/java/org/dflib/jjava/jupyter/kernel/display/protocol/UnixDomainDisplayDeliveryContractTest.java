package org.dflib.jjava.jupyter.kernel.display.protocol;

import org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayDelivery;
import org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class UnixDomainDisplayDeliveryContractTest extends DisplayDeliveryContractTest {

    private final List<UnixDomainDisplayEndpoint> endpoints = new ArrayList<>();

    @Override
    protected DisplayDelivery delivery(DisplayRequestHandler handler) {
        try {
            UnixDomainDisplayEndpoint endpoint = new UnixDomainDisplayEndpoint(handler);
            endpoints.add(endpoint);
            return new UnixDomainDisplayDelivery(endpoint.path());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterEach
    void closeEndpoints() throws IOException {
        for (UnixDomainDisplayEndpoint endpoint : endpoints) {
            endpoint.close();
        }
    }
}
