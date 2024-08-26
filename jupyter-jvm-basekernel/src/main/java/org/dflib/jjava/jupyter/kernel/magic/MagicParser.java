package org.dflib.jjava.jupyter.kernel.magic;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MagicParser {
    protected static List<String> split(String args) {
        args = args.trim();

        List<String> split = new LinkedList<>();

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
                        current.append("\\\\");
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
                        if (current.length() > 0 && inQuotes) {
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

    private final Pattern lineMagicPattern;
    private final Pattern cellMagicPattern;

    public MagicParser() {
        this("^%", "%%");
    }

    public MagicParser(String lineMagicStart, String cellMagicStart) {
        this.lineMagicPattern = Pattern.compile(lineMagicStart + "(?<args>\\w.*?)$", Pattern.MULTILINE);
        this.cellMagicPattern = Pattern.compile("^(?<argsLine>" + cellMagicStart + "(?<args>\\w.*?))\\R(?<body>(?sU).+?)$");
    }

    public String transformLineMagics(String cell, Function<LineMagicParseContext, String> transformer) {
        StringBuffer transformedCell = new StringBuffer();

        Matcher m = this.lineMagicPattern.matcher(cell);
        while (m.find()) {
            String raw = m.group();
            String rawArgs = m.group("args");
            List<String> split = split(rawArgs);

            LineMagicArgs args = LineMagicArgs.of(split.get(0), split.subList(1, split.size()));
            LineMagicParseContext ctx = LineMagicParseContext.of(args, raw, cell, cell.substring(0, m.start()));

            String transformed = transformer.apply(ctx);
            if (transformed == null) transformed = raw;

            m.appendReplacement(transformedCell, Matcher.quoteReplacement(transformed));
        }
        m.appendTail(transformedCell);

        return transformedCell.toString();
    }

    public CellMagicParseContext parseCellMagic(String cell) {
        Matcher m = this.cellMagicPattern.matcher(cell);

        if (!m.matches()) return null;

        String rawArgsLine = m.group("argsLine");
        String rawArgs = m.group("args");
        String body = m.group("body");
        List<String> split = split(rawArgs);

        CellMagicArgs args = CellMagicArgs.of(split.get(0), split.subList(1, split.size()), body);
        return CellMagicParseContext.of(args, rawArgsLine, cell);
    }

    public String transformCellMagic(String cell, Function<CellMagicParseContext, String> transformer) {
        CellMagicParseContext ctx = this.parseCellMagic(cell);

        return ctx == null ? cell : transformer.apply(ctx);
    }
}
