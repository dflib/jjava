package org.dflib.jjava.kernel;

import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.ExecutorNotebookStatics;
import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standalone magics are dispatched in the manager. If the generated call had been sent to the executor, the executor
 * helper would throw UnsupportedOperationException, so a normal result also shows the executor never received it.
 * Inline magics are the opposite: they resolve to an executor helper call.
 */
class StandaloneMagicDispatchTest {

    @Test
    void standaloneLineAndCellMagicsDispatchInTheManager() {
        List<BaseKernel> seen = new ArrayList<>();
        LineMagic<String, BaseKernel> line = (kernel, args) -> {
            seen.add(kernel);
            return "line";
        };
        CellMagic<String, BaseKernel> cell = (kernel, args, body) -> {
            seen.add(kernel);
            return body;
        };

        JavaKernel kernel = JavaKernel.builder()
                .extensionsEnabled(false)
                .lineMagic("echoLine", line)
                .cellMagic("echoCell", cell)
                .build();
        try {
            assertEquals("line", kernel.evalBuilder("%echoLine hi").resolveMagics().eval());
            assertEquals("body", kernel.evalBuilder("%%echoCell x\nbody").resolveMagics().eval());

            assertEquals(2, seen.size());
            assertSame(kernel, seen.get(0));
            assertSame(kernel, seen.get(1));

            String inline = kernel.getMagicsResolver().resolve("x = %echoLine hi");
            assertTrue(inline.contains(ExecutorNotebookStatics.class.getName() + ".lineMagic("), inline);
        } finally {
            kernel.onShutdown(false);
        }
    }
}
