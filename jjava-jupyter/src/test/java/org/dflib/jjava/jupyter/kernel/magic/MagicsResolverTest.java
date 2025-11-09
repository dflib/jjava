package org.dflib.jjava.jupyter.kernel.magic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MagicsResolverTest {

    @Test
    public void parseCellMagic() {
        String cell = "//%%cellMagicName arg1 \"arg2 arg2\" arg3  \n" +
                "This is the body\n" +
                "with multiple lines";

        ParsedCellMagic parsed = inlineParser(emptyTranspiler).parseCellMagic(cell);

        assertNotNull(parsed);
        assertEquals("cellMagicName", parsed.name);
        assertEquals(Arrays.asList("arg1", "arg2 arg2", "arg3"), parsed.args);
        assertEquals("This is the body\nwith multiple lines", parsed.cellBodyAfterMagic);
    }

    @Test
    public void resolveLineMagics() {
        String cell = "//%magicName arg1 arg2\n" +
                "Inline magic = //%magicName2 arg1\n" +
                "//Just a comment\n" +
                "//%magicName3 arg1 \"arg2 arg2\"\n" +
                "//%magicName4 escaped\\\\backslash escaped\\\"quote\n" +
                "//%magicName5 \"quoted-escaped\\\\backslash\" \"quoted-escaped\\\"quote\"\n" +
                "//%magicName6 \"\" quoted-empty-string";

        String transpiled = inlineParser(joinTranspiler).resolveLineMagics(cell);

        String transpiledExpected = "**magicName-arg1,arg2\n" +
                "Inline magic = **magicName2-arg1\n" +
                "//Just a comment\n" +
                "**magicName3-arg1,arg2 arg2\n" +
                "**magicName4-escaped\\backslash,escaped\"quote\n" +
                "**magicName5-quoted-escaped\\backslash,quoted-escaped\"quote\n" +
                "**magicName6-,quoted-empty-string";

        assertEquals(transpiledExpected, transpiled);
    }

    @Test
    public void resolveLineMagics_startOfLineParserSkipsInline1() {
        String cell = "System.out.printf(\"Fmt //%s string\", \"test\");";
        String transpiled = startOfLineParser(emptyTranspiler).resolveLineMagics(cell);
        assertEquals(cell, transpiled);
    }

    @Test
    public void resolveLineMagics_startOfLineParserSkipsInline2() {
        String cell = String.join("\n",
                "//%sol",
                "Not //%sol"
        );

        String transformedCell = startOfLineParser(nameTranspiler).resolveLineMagics(cell);
        String expectedTransformedCell = String.join("\n",
                "sol",
                "Not //%sol"
        );

        assertEquals(expectedTransformedCell, transformedCell);
    }

    @Test
    public void transpileLineMagics_startOfLineParserAllowsWhitespace() {
        String cell = String.join("\n",
                "//%sol",
                "  //%sol2",
                "\t//%sol3"
        );

        String transformedCell = startOfLineParser(nameTranspiler).resolveLineMagics(cell);
        String expectedTransformedCell = String.join("\n",
                "sol",
                "sol2",
                "sol3"
        );

        assertEquals(expectedTransformedCell, transformedCell);
    }

    static MagicsResolver inlineParser(MagicTranspiler transpiler) {
        return new MagicsResolver("//%", "//%%", transpiler);
    }

    static MagicsResolver startOfLineParser(MagicTranspiler transpiler) {
        return new MagicsResolver("^\\s*//%", "//%%", transpiler);
    }

    static final MagicTranspiler emptyTranspiler = new MagicTranspiler() {
        @Override
        public String transpileCell(ParsedCellMagic magic) {
            return "";
        }

        @Override
        public String transpileLine(ParsedLineMagic magic) {
            return "";
        }
    };

    static final MagicTranspiler joinTranspiler = new MagicTranspiler() {

        @Override
        public String transpileCell(ParsedCellMagic magic) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public String transpileLine(ParsedLineMagic magic) {
            return "**" + magic.name + "-" + String.join(",", magic.args);
        }
    };

    static final MagicTranspiler nameTranspiler = new MagicTranspiler() {

        @Override
        public String transpileCell(ParsedCellMagic magic) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public String transpileLine(ParsedLineMagic magic) {
            return magic.name;
        }
    };
}
