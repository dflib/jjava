package org.dflib.jjava.jupyter.kernel.magic;

import org.dflib.jjava.jupyter.kernel.ExecutorNotebookStatics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MagicTranspilerTest {

    private static final String EXECUTOR = ExecutorNotebookStatics.class.getName();

    @Test
    public void generatedCellAndLineCallsTargetExecutorHelpers() {
        MagicTranspiler transpiler = new MagicTranspiler();

        String cell = transpiler.transpileCell(new ParsedCellMagic("m", List.of(), "body"));
        String line = transpiler.transpileLine(new ParsedLineMagic("m", List.of(), "x = ", "%m"));

        assertTrue(cell.startsWith(EXECUTOR + ".cellMagic("), cell);
        assertTrue(line.startsWith(EXECUTOR + ".lineMagic("), line);
        assertFalse(cell.contains("BaseNotebookStatics") || line.contains("BaseNotebookStatics"));
    }
}
