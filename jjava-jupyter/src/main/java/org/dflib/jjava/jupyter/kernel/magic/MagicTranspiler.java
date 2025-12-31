package org.dflib.jjava.jupyter.kernel.magic;

import org.dflib.jjava.jupyter.kernel.BaseNotebookStatics;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A converter (aka "transpiler") of generic syntax of a single magic into kernel-specific syntax (such as Java).
 */
public class MagicTranspiler {

    private static final Pattern UNESCAPED_QUOTE = Pattern.compile("(?<!\\\\)\"");

    // generated code templates
    private static final String CELL_CALL_TEMPLATE = BaseNotebookStatics.class.getName() + ".cellMagic(%s,java.util.List.of(%s),%s);";
    private static final String LINE_CALL_TEMPLATE = BaseNotebookStatics.class.getName() + ".lineMagic(%s,java.util.List.of(%s));";
    private static final String DECODE_TEMPLATE = "new String(java.util.Base64.getDecoder().decode(\"%s\"))";

    public String transpileCell(ParsedCellMagic magic) {
        return String.format(CELL_CALL_TEMPLATE,
                argWithEscapingToJava(magic.name),
                magic.args.stream()
                        .map(this::argWithEscapingToJava)
                        .collect(Collectors.joining(",")),
                argWithEscapingToJava(magic.cellBodyAfterMagic)
        );
    }

    public String transpileLine(ParsedLineMagic magic) {
        boolean inString = false;
        Matcher m = UNESCAPED_QUOTE.matcher(magic.magicLinePrefix);
        while (m.find()) {
            inString = !inString;
        }

        // If in a string literal, don't apply the magic, just use the original
        if (inString) {
            return magic.unparsedMagic;
        }

        return String.format(LINE_CALL_TEMPLATE,
                argWithEscapingToJava(magic.name),
                magic.args.stream()
                        .map(this::argWithEscapingToJava)
                        .collect(Collectors.joining(","))
        );
    }

    // Poor man's string escape
    private String argWithEscapingToJava(String arg) {
        String encoded = Base64.getEncoder().encodeToString(arg.getBytes());
        return String.format(DECODE_TEMPLATE, encoded);
    }
}
