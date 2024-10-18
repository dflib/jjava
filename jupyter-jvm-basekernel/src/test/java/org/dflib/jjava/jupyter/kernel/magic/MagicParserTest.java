package org.dflib.jjava.jupyter.kernel.magic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class MagicParserTest {
    private MagicParser inlineParser;
    private MagicParser solParser;

    @BeforeEach
    public void setUp() {
        this.inlineParser = new MagicParser("//%", "//%%");
        this.solParser = new MagicParser("^\\s*//%", "//%%");
    }

    @Test
    public void transformLineMagics() {
        String cell = String.join("\n",
                "//%magicName arg1 arg2",
                "Inline magic = //%magicName2 arg1",
                "//Just a comment",
                "//%magicName3 arg1 \"arg2 arg2\""
        );

        String transformedCell = this.inlineParser.transformLineMagics(cell, ctx ->
                "**" + ctx.getMagicCall().getName() + "-" + String.join(",", ctx.getMagicCall().getArgs())
        );

        String expectedTransformedCell = String.join("\n",
                "**magicName-arg1,arg2",
                "Inline magic = **magicName2-arg1",
                "//Just a comment",
                "**magicName3-arg1,arg2 arg2"
        );

        Assertions.assertEquals(expectedTransformedCell, transformedCell);
    }

    @Test
    public void parseCellMagic() {
        String cell = String.join("\n",
                "//%%cellMagicName arg1 \"arg2 arg2\" arg3  ",
                "This is the body",
                "with multiple lines"
        );

        CellMagicParseContext ctx = this.inlineParser.parseCellMagic(cell);

        Assertions.assertNotNull(ctx);
        Assertions.assertEquals("cellMagicName", ctx.getMagicCall().getName());
        Assertions.assertEquals(Arrays.asList("arg1", "arg2 arg2", "arg3"), ctx.getMagicCall().getArgs());
        Assertions.assertEquals("This is the body\nwith multiple lines", ctx.getMagicCall().getBody());
        Assertions.assertEquals("//%%cellMagicName arg1 \"arg2 arg2\" arg3  ", ctx.getRawArgsLine());
        Assertions.assertEquals(cell, ctx.getRawCell());
    }

    @Test
    public void transformCellMagic() {
        String cell = String.join("\n",
                "//%%cellMagicName arg1 \"arg2 arg2\" arg3  ",
                "This is the body",
                "with multiple lines"
        );

        String transformedCell = this.inlineParser.transformCellMagic(cell, ctx ->
                ctx.getMagicCall().getName() + "(" + String.join(",", ctx.getMagicCall().getArgs()) + ")" + "\n"
                        + ctx.getMagicCall().getBody()
        );

        String expectedTransformedCell = "cellMagicName(arg1,arg2 arg2,arg3)\nThis is the body\nwith multiple lines";

        Assertions.assertEquals(expectedTransformedCell, transformedCell);
    }

    @Test
    public void dontTransformNonMagicCell() {
        String cell = String.join("\n",
                "//%cellMagicName arg1 \"arg2 arg2\" arg3  ",
                "This is the body",
                "with multiple lines"
        );

        String transformedCell = this.inlineParser.transformCellMagic(cell, ctx -> "transformer applied");

        Assertions.assertEquals(cell, transformedCell);
    }

    @Test
    public void startOfLineParserSkipsInlineMagics() {
        String cell = "System.out.printf(\"Fmt //%s string\", \"test\");";

        String transformedCell = this.solParser.transformLineMagics(cell, ctx -> "");

        Assertions.assertEquals(cell, transformedCell);
    }

    @Test
    public void startOfLineParserAllowsWhitespace() {
        String cell = String.join("\n",
                "//%sol",
                "  //%sol2",
                "\t//%sol3"
        );

        String transformedCell = this.solParser.transformLineMagics(cell, ctx -> ctx.getMagicCall().getName());
        String expectedTransformedCell = String.join("\n",
                "sol",
                "sol2",
                "sol3"
        );

        Assertions.assertEquals(expectedTransformedCell, transformedCell);
    }

    @Test
    public void startOfLineParserSkipsInline() {
        String cell = String.join("\n",
                "//%sol",
                "Not //%sol"
        );

        String transformedCell = this.solParser.transformLineMagics(cell, ctx -> ctx.getMagicCall().getName());
        String expectedTransformedCell = String.join("\n",
                "sol",
                "Not //%sol"
        );

        Assertions.assertEquals(expectedTransformedCell, transformedCell);
    }

    public static List<String> split(String args) {
        return MagicParser.split(args);
    }
}
