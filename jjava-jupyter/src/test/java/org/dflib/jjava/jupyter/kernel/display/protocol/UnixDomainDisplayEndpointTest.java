package org.dflib.jjava.jupyter.kernel.display.protocol;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.protocol.uds.UnixDomainDisplayEndpoint;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnixDomainDisplayEndpointTest {

    @Test
    public void servesOneRequestLinePerConnectionInOrderAndRemovesSocketOnShutdown() throws Exception {
        List<String> ids = Collections.synchronizedList(new ArrayList<>());
        UnixDomainDisplayEndpoint endpoint = new UnixDomainDisplayEndpoint(r -> ids.add(r.displayId()));
        Path socket = endpoint.path();
        try {
            assertEquals("ok", send(socket, line("first")));
            assertEquals("ok", send(socket, line("second")));
            assertEquals(List.of("first", "second"), ids);
            assertTrue(send(socket, "not json").startsWith("\""));
        } finally {
            endpoint.close();
        }
        assertFalse(Files.exists(socket));
    }

    private static String line(String id) {
        return DisplayProtocol.encode(DisplayRequest.of(DisplayRequest.Operation.DISPLAY, id, new DisplayData("x")));
    }

    private static String send(Path socket, String line) throws IOException {
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(UnixDomainSocketAddress.of(socket));
            channel.write(ByteBuffer.wrap((line + "\n").getBytes(StandardCharsets.UTF_8)));

            ByteArrayOutputStream response = new ByteArrayOutputStream();
            ByteBuffer one = ByteBuffer.allocate(1);
            while (channel.read(one.clear()) > 0 && one.get(0) != '\n') {
                response.write(one.get(0));
            }
            return response.toString(StandardCharsets.UTF_8);
        }
    }
}
