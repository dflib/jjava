package org.dflib.jjava.jupyter.kernel.magic.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MagicsTest {
    private Magics magics;

    @BeforeEach
    public void setUp() {
        magics = new Magics();
    }

    @Test
    public void lineMagic() throws Exception {
        magics.registerLineMagic("test", args -> args);

        List<String> args = Arrays.asList("arg1", "arg2");
        List<String> out = magics.applyLineMagic("test", args);

        assertEquals(args, out);
    }

    @Test
    public void cellMagic() throws Exception {
        magics.registerCellMagic("test", (args, body) -> {
            List<String> out = new ArrayList<>(args);
            out.add(body);
            return out;
        });

        List<String> args = Arrays.asList("arg1", "arg2");
        String body = "body";
        List<String> out = magics.applyCellMagic("test", args, body);

        List<String> expected = new ArrayList<>(args);
        expected.add(body);

        assertEquals(expected, out);
    }

    @Test
    public void lineCellMagic() throws Exception {
        class Magic implements LineMagicFunction<List<String>>, CellMagicFunction<List<String>> {
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

        magics.registerLineCellMagic("test", new Magic());

        List<String> args = Arrays.asList("arg1", "arg2");
        String body = "body";

        List<String> lineOut = magics.applyLineMagic("test", args);
        List<String> cellOut = magics.applyCellMagic("test", args, body);

        List<String> expectedCell = new ArrayList<>(args);
        expectedCell.add(body);

        assertEquals(args, lineOut);
        assertEquals(expectedCell, cellOut);
    }

    @Test
    public void reflectionLineMagics() throws Exception {
        class Magic {
            @LineMagic
            public void list(List<String> args) {
            }

            @LineMagic
            public void iterable(Iterable<String> args) {
            }

            @LineMagic("named")
            public void unusedName(List<String> args) {
            }

            @LineMagic
            public int returnInt(List<String> args) {
                return args.size();
            }
        }

        magics.registerMagics(new Magic());

        List<String> args = Arrays.asList("arg1", "arg2");

        magics.applyLineMagic("list", args);
        magics.applyLineMagic("iterable", args);
        magics.applyLineMagic("named", args);
        assertEquals((Integer) 2, magics.applyLineMagic("returnInt", args));

        try {
            magics.applyLineMagic("unusedName", args);
            fail("named magic was also registered under the method name");
        } catch (UndefinedMagicException ignored) {
        }
    }

    @Test
    public void badReflectionLineMagicsType() {
        class BadMagic {
            @LineMagic
            public void set(Set<String> args) {
            }
        }

        assertThrows(IllegalArgumentException.class, () -> magics.registerMagics(new BadMagic()));
    }

    @Test
    public void badReflectionLineMagicsTypeParam() {
        class BadMagic {
            @LineMagic
            public void intList(List<Integer> args) {
            }
        }

        assertThrows(IllegalArgumentException.class, () -> magics.registerMagics(new BadMagic()));
    }

    @Test
    public void reflectionCellMagics() throws Exception {
        class Magic {
            @CellMagic
            public void list(List<String> args, String body) {
            }

            @CellMagic
            public void iterable(Iterable<String> args, String body) {
            }

            @CellMagic("named")
            public void unusedName(List<String> args, String body) {
            }

            @CellMagic
            public int returnInt(List<String> args, String body) {
                return args.size();
            }
        }

        magics.registerMagics(new Magic());

        List<String> args = Arrays.asList("arg1", "arg2");
        String body = "body";

        magics.applyCellMagic("list", args, body);
        magics.applyCellMagic("iterable", args, body);
        magics.applyCellMagic("named", args, body);
        assertEquals((Integer) 2, magics.applyCellMagic("returnInt", args, body));

        try {
            magics.applyCellMagic("unusedName", args, body);
            fail("named magic was also registered under the method name");
        } catch (UndefinedMagicException ignored) {
        }
    }

    @Test
    public void badReflectionCellMagicsType() {
        class BadMagic {
            @LineMagic
            public void set(Set<String> args, String body) {
            }
        }

        assertThrows(IllegalArgumentException.class, () -> magics.registerMagics(new BadMagic()));
    }

    @Test
    public void badReflectionCellMagicsTypeParam() {
        class BadMagic {
            @LineMagic
            public void intList(List<Integer> args, String body) {
            }
        }

        assertThrows(IllegalArgumentException.class, () -> magics.registerMagics(new BadMagic()));
    }

    @Test
    public void badReflectionCellMagicsBodyTypeParam() {
        class BadMagic {
            @LineMagic
            public void body(List<String> args, Character body) {
            }
        }

        assertThrows(IllegalArgumentException.class, () -> magics.registerMagics(new BadMagic()));
    }

    @Test
    public void reflectionLineCellMagics() throws Exception {
        class Magic {
            @LineMagic
            @CellMagic
            public void list(List<String> args, String body) {
            }

            @LineMagic
            @CellMagic
            public void iterable(Iterable<String> args, String body) {
            }

            @LineMagic("lineNamed")
            @CellMagic("cellNamed")
            public void unusedName(List<String> args, String body) {
            }

            @LineMagic
            @CellMagic
            public int returnInt(List<String> args, String body) {
                return args.size() + (body == null ? 1 : 2);
            }
        }

        magics.registerMagics(new Magic());

        List<String> args = Arrays.asList("arg1", "arg2");
        String body = "body";

        magics.applyLineMagic("list", args);
        magics.applyLineMagic("iterable", args);
        magics.applyLineMagic("lineNamed", args);
        assertEquals((Integer) 3, magics.applyLineMagic("returnInt", args));

        magics.applyCellMagic("list", args, body);
        magics.applyCellMagic("iterable", args, body);
        magics.applyCellMagic("cellNamed", args, body);
        assertEquals((Integer) 4, magics.applyCellMagic("returnInt", args, body));

        try {
            magics.applyCellMagic("unusedName", args, body);
            fail("named magic was also registered under the method name");
        } catch (UndefinedMagicException ignored) {
        }
    }

    @Test
    public void statefulMagic() throws Exception {
        class Magic {
            private int i = 0;

            @LineMagic
            public int getAndIncrement() {
                return i++;
            }
        }

        magics.registerMagics(new Magic());

        for (int i = 0; i < 3; i++)
            assertEquals((Integer) i, magics.applyLineMagic("getAndIncrement", Collections.emptyList()));
    }

    @Test
    public void staticMagic() throws Exception {
        magics.registerMagics(StaticMagics.class);

        assertEquals((Integer) 0, magics.applyLineMagic("staticMagic", Collections.emptyList()));
        assertEquals("body", magics.applyCellMagic("staticMagic", Collections.emptyList(), "body"));
    }
}
