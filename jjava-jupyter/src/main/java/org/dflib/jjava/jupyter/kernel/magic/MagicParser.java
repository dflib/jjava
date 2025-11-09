package org.dflib.jjava.jupyter.kernel.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locates line and cell magic syntax in the cell code and replaces it with code native to the underlying kernel.
 */
public class MagicParser {

    private final Pattern lineMagicPattern;
    private final Pattern cellMagicPattern;
    private final MagicTranspiler transpiler;

    public MagicParser(String lineMagicStart, String cellMagicStart, MagicTranspiler transpiler) {
        this.lineMagicPattern = Pattern.compile(lineMagicStart + "(?<args>\\w.*?)$", Pattern.MULTILINE);
        this.cellMagicPattern = Pattern.compile("^(?<argsLine>" + cellMagicStart + "(?<args>\\w.*?))\\R(?<body>(?sU).+?)$");
        this.transpiler = transpiler;
    }

    /**
     * Replaces cell and line magics in the source with native kernel code.
     */
    public String resolveMagics(String cellSource) {
        return transpileCellMagic(cellSource).orElse(transpileLineMagics(cellSource));
    }

    private Optional<String> transpileCellMagic(String cellSource) {
        ParsedCellMagic parsedCell = parseCellMagic(cellSource);
        return Optional.ofNullable(parsedCell).map(transpiler::transpileCell);
    }

    String transpileLineMagics(String cellSource) {

        StringBuffer out = new StringBuffer();
        Matcher m = lineMagicPattern.matcher(cellSource);

        while (m.find()) {
            ParsedLineMagic parsed = parseLineMagic(cellSource, m);
            String transformed = transpiler.transpileLine(parsed);
            m.appendReplacement(out, Matcher.quoteReplacement(transformed));
        }

        m.appendTail(out);
        return out.toString();
    }

    ParsedCellMagic parseCellMagic(String cellSource) {
        Matcher m = cellMagicPattern.matcher(cellSource);

        if (!m.matches()) {
            return null;
        }

        List<String> split = split(m.group("args"));
        String body = m.group("body");

        return new ParsedCellMagic(
                split.get(0),
                split.subList(1, split.size()),
                body);
    }

    private ParsedLineMagic parseLineMagic(String cellSource, Matcher matchedLine) {
        List<String> split = split(matchedLine.group("args"));

        String rawLinePrefix = cellSource.substring(0, matchedLine.start());
        String linePrefix = rawLinePrefix.substring(rawLinePrefix.lastIndexOf('\n') + 1);

        return new ParsedLineMagic(
                split.get(0),
                split.subList(1, split.size()),
                linePrefix,
                matchedLine.group()
        );
    }

    static List<String> split(String args) {
        args = args.trim();

        List<String> split = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;
        for (char c : args.toCharArray()) {
            switch (c) {
                case ' ':
                case '\t':
                    if (inQuotes) {
                        current.append(c);
                    } else if (current.length() > 0) {
                        // If whitespace is closing the string the add the current and reset
                        split.add(current.toString());
                        current.setLength(0);
                    }
                    break;
                case '\\':
                    if (escape) {
                        current.append("\\");
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                case '\"':
                    if (escape) {
                        current.append('"');
                        escape = false;
                    } else {
                        if (inQuotes) {
                            split.add(current.toString());
                            current.setLength(0);
                            inQuotes = false;
                        } else {
                            inQuotes = true;
                        }
                    }
                    break;
                default:
                    current.append(c);
            }
        }

        if (current.length() > 0) {
            split.add(current.toString());
        }

        return split;
    }
}
