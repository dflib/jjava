package org.dflib.jjava.jupyter.kernel.magic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MagicsRegistryTest {

    @Test
    public void lineMagic() throws Exception {
        MagicsRegistry registry = new MagicsRegistry(Map.of("test", args -> args), Map.of());

        List<String> args = Arrays.asList("arg1", "arg2");
        List<String> out = registry.evalLineMagic("test", args);

        assertEquals(args, out);
    }

    @Test
    public void cellMagic() throws Exception {
        MagicsRegistry registry = new MagicsRegistry(Map.of(), Map.of("test", (args, body) -> {
            List<String> out = new ArrayList<>(args);
            out.add(body);
            return out;
        }));

        List<String> args = Arrays.asList("arg1", "arg2");
        String body = "body";
        List<String> out = registry.evalCellMagic("test", args, body);

        List<String> expected = new ArrayList<>(args);
        expected.add(body);

        assertEquals(expected, out);
    }

    @Test
    public void lineCellMagic() throws Exception {
        class Magic implements LineMagic<List<String>>, CellMagic<List<String>> {
            @Override
            public List<String> execute(List<String> args, String body) {
                List<String> out = new ArrayList<>(args);
                out.add(body);
                return out;
            }

            @Override
            public List<String> execute(List<String> args) {
                return args;
            }
        }

        Magic magic = new Magic();
        MagicsRegistry registry = new MagicsRegistry(Map.of("test", magic), Map.of("test", magic));

        List<String> args = Arrays.asList("arg1", "arg2");
        String body = "body";

        List<String> lineOut = registry.evalLineMagic("test", args);
        List<String> cellOut = registry.evalCellMagic("test", args, body);

        List<String> expectedCell = new ArrayList<>(args);
        expectedCell.add(body);

        assertEquals(args, lineOut);
        assertEquals(expectedCell, cellOut);
    }
}
