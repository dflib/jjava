package org.dflib.jjava.kernel;

import jdk.jshell.JShell;
import org.dflib.jjava.kernel.execution.CodeEvaluator;
import org.dflib.jjava.kernel.execution.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputBoundaryDependencyTest {

    @Test
    public void evaluatorAndResultModelDoNotDependOnJupyterPresentation() {
        List<String> violations = new ArrayList<>();
        List<String[]> edges = edges(Path.of("target", "classes"));
        assertFalse(edges.isEmpty(), "jdeps produced no edges; the check would pass vacuously");
        for (String[] edge : edges) {
            String from = edge[0];
            String to = edge[1];
            boolean evaluator = from.startsWith(CodeEvaluator.class.getName());
            boolean result = from.startsWith(EvaluationResult.class.getName());
            if ((evaluator || result) && (to.startsWith("org.dflib.jjava.jupyter.kernel.display.")
                    || to.startsWith("org.dflib.jjava.jupyter.messages.")
                    || to.startsWith("org.dflib.jjava.jupyter.channels."))) {
                violations.add(from + " -> " + to);
            }
        }
        assertTrue(violations.isEmpty(), "forbidden edges: " + violations);
    }

    @Test
    public void evalReturnsEvaluationResult() throws Exception {
        assertEquals(EvaluationResult.class,
                CodeEvaluator.class.getMethod("eval", JShell.class, String.class).getReturnType());
    }

    private static List<String[]> edges(Path classes) {
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
            edges.add(new String[]{
                    line.substring(0, arrow).trim(),
                    line.substring(arrow + 4).trim().split("\\s+")[0]});
        }
        return edges;
    }
}
