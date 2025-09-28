package org.dflib.jjava.jupyter.kernel.magic;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A converter (aka "transpiler") of the generic magic syntax into kernel-specific syntax (such as Java).
 */
public class MagicTranspiler {

    private static final Pattern UNESCAPED_QUOTE = Pattern.compile("(?<!\\\\)\"");

    public String transpileCell(ParsedCellMagic magic) {
        return String.format(
                "org.dflib.jjava.jupyter.kernel.NotebookStatics.cellMagic(%s,java.util.List.of(%s),%s);{};",
                argWithEscapingToJava(magic.magicCall.name),
                magic.magicCall.args.stream()
                        .map(this::argWithEscapingToJava)
                        .collect(Collectors.joining(",")),
                argWithEscapingToJava(magic.magicCall.body)
        );
    }

    public String transpileLine(ParsedLineMagic magic) {
        boolean inString = false;
        Matcher m = UNESCAPED_QUOTE.matcher(magic.linePrefix);
        while (m.find()) {
            inString = !inString;
        }

        // If in a string literal, don't apply the magic, just use the original
        if (inString) {
            return magic.raw;
        }

        return String.format(
                "org.dflib.jjava.jupyter.kernel.NotebookStatics.lineMagic(%s,java.util.List.of(%s));{};",
                argWithEscapingToJava(magic.magicCall.name),
                magic.magicCall.args.stream()
                        .map(this::argWithEscapingToJava)
                        .collect(Collectors.joining(","))
        );
    }

    // Poor man's string escape
    private String argWithEscapingToJava(String arg) {
        String encoded = Base64.getEncoder().encodeToString(arg.getBytes());
        return String.format("new String(java.util.Base64.getDecoder().decode(\"%s\"))", encoded);
    }
}
