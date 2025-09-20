package org.dflib.jjava.execution;

import org.dflib.jjava.jupyter.kernel.magic.CellMagicParseContext;
import org.dflib.jjava.jupyter.kernel.magic.LineMagicParseContext;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MagicsSourceTransformer {
    private static final Pattern UNESCAPED_QUOTE = Pattern.compile("(?<!\\\\)\"");

    private final MagicParser parser;

    public MagicsSourceTransformer() {
        this.parser = new MagicParser("(?<=(?:^|=))\\s*%", "%%");
    }

    public String transformMagics(String source) {
        CellMagicParseContext ctx = this.parser.parseCellMagic(source);
        if (ctx != null)
            return this.transformCellMagic(ctx);

        return transformLineMagics(source);
    }

    public String transformLineMagics(String source) {
        return this.parser.transformLineMagics(source, ctx -> {
            boolean inString = false;
            Matcher m = UNESCAPED_QUOTE.matcher(ctx.getLinePrefix());
            while (m.find())
                inString = !inString;

            // If in a string literal, don't apply the magic, just use the original
            if (inString)
                return ctx.getRaw();

            return transformLineMagic(ctx);
        });
    }

    // Poor mans string escape
    private String b64Transform(String arg) {
        String encoded = Base64.getEncoder().encodeToString(arg.getBytes());

        return String.format("new String(java.util.Base64.getDecoder().decode(\"%s\"))", encoded);
    }

    private String transformLineMagic(LineMagicParseContext ctx) {
        return String.format(
                "org.dflib.jjava.runtime.Magics.lineMagic(%s,java.util.List.of(%s));{};",
                this.b64Transform(ctx.getMagicCall().getName()),
                ctx.getMagicCall().getArgs().stream()
                        .map(this::b64Transform)
                        .collect(Collectors.joining(","))
        );
    }

    private String transformCellMagic(CellMagicParseContext ctx) {
        return String.format(
                "org.dflib.jjava.runtime.Magics.cellMagic(%s,java.util.List.of(%s),%s);{};",
                this.b64Transform(ctx.getMagicCall().getName()),
                ctx.getMagicCall().getArgs().stream()
                        .map(this::b64Transform)
                        .collect(Collectors.joining(",")),
                this.b64Transform(ctx.getMagicCall().getBody())
        );
    }
}
