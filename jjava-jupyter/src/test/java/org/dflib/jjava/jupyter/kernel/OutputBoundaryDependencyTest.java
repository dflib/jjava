package org.dflib.jjava.jupyter.kernel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputBoundaryDependencyTest {

    private static final String PROTOCOL = "org.dflib.jjava.jupyter.kernel.display.protocol.";
    private static final String BASE_STATICS = "org.dflib.jjava.jupyter.kernel.BaseNotebookStatics";

    @Test
    public void protocolClassesOutsideTransportPackagesDoNotUseSocketsOrJupyterChannels() {
        List<String> violations = new ArrayList<>();
        List<String[]> edges = edges(Path.of("target", "classes"));
        assertFalse(edges.isEmpty(), "jdeps produced no edges; the check would pass vacuously");
        for (String[] edge : edges) {
            String from = edge[0];
            String to = edge[1];
            if (isDirectProtocolClass(from) && (to.startsWith("java.net.")
                    || to.startsWith("java.nio.channels.")
                    || to.startsWith("org.dflib.jjava.jupyter.channels.")
                    || to.startsWith("org.dflib.jjava.jupyter.messages.")
                    || to.equals("org.dflib.jjava.jupyter.kernel.DisplayStream")
                    || to.equals("org.dflib.jjava.jupyter.kernel.BaseKernel"))) {
                violations.add(from + " -> " + to);
            }
        }
        assertTrue(violations.isEmpty(), "forbidden edges: " + violations);
    }

    @Test
    public void managerHelperNeverDeliversThroughTheExecutorBoundary() {
        List<String> violations = new ArrayList<>();
        for (String[] edge : edges(Path.of("target", "classes"))) {
            String from = edge[0];
            String to = edge[1];
            if (from.startsWith(BASE_STATICS) && (to.startsWith(PROTOCOL + "uds.")
                    || to.startsWith(PROTOCOL + "tcp.")
                    || to.equals(PROTOCOL + "DisplayDelivery")
                    || to.equals(PROTOCOL + "DisplayDeliveryBootstrap")
                    || to.startsWith("java.net."))) {
                violations.add(from + " -> " + to);
            }
        }
        assertTrue(violations.isEmpty(), "forbidden edges: " + violations);
    }

    // A class directly in the protocol package (not in uds/tcp subpackages).
    private static boolean isDirectProtocolClass(String name) {
        return name.startsWith(PROTOCOL) && name.indexOf('.', PROTOCOL.length()) < 0;
    }

    static List<String[]> edges(Path classes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ToolProvider.findFirst("jdeps").orElseThrow().run(
                new PrintStream(out, true, StandardCharsets.UTF_8), System.err,
                "-verbose:class", classes.toString());

        List<String[]> edges = new ArrayList<>();
        for (String line : out.toString(StandardCharsets.UTF_8).split("\n")) {
            int arrow = line.indexOf(" -> ");
            if (arrow < 0) {
                continue;
            }
            String from = line.substring(0, arrow).trim();
            String to = line.substring(arrow + 4).trim().split("\\s+")[0];
            edges.add(new String[]{from, to});
        }
        return edges;
    }
}
