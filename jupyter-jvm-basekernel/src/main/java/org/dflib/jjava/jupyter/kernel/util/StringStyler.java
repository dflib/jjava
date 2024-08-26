package org.dflib.jjava.jupyter.kernel.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringStyler {
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R|$");

    public static List<String> splitLines(String src) {
        return Arrays.asList(NEWLINE_PATTERN.split(src));
    }

    public static class Builder {
        private String primaryStyle = "";
        private String secondaryStyle = "";
        private String hlStyle = "";
        private Function<Integer, String> linePrefix = null;

        public Builder addPrimaryStyle(String style) {
            this.primaryStyle += style;
            return this;
        }

        public Builder addPrimaryStyle(TextColor style) {
            this.primaryStyle += style;
            return this;
        }

        public Builder addSecondaryStyle(String style) {
            this.secondaryStyle += style;
            return this;
        }

        public Builder addSecondaryStyle(TextColor style) {
            this.secondaryStyle += style;
            return this;
        }

        public Builder addHighlightStyle(String style) {
            this.hlStyle += style;
            return this;
        }

        public Builder addHighlightStyle(TextColor style) {
            this.hlStyle += style;
            return this;
        }

        public Builder withLinePrefix(Function<Integer, String> linePrefix) {
            this.linePrefix = linePrefix;
            return this;
        }

        public Builder withLinePrefix(String linePrefix) {
            this.linePrefix = line -> linePrefix;
            return this;
        }

        public StringStyler build() {
            return new StringStyler(
                    this.primaryStyle,
                    this.secondaryStyle,
                    this.hlStyle,
                    this.linePrefix != null ? this.linePrefix : line -> ""
            );
        }
    }

    private String primaryStyle;
    private String secondaryStyle;
    private String hlStyle;
    private Function<Integer, String> linePrefix;

    public StringStyler(String primaryStyle, String secondaryStyle, String hlStyle, Function<Integer, String> linePrefix) {
        this.primaryStyle = primaryStyle;
        this.secondaryStyle = secondaryStyle;
        this.hlStyle = hlStyle;
        this.linePrefix = linePrefix;
    }

    public String primary(String src) {
        return this.primaryStyle + src + TextColor.RESET_ALL;
    }

    public String secondary(String src) {
        return this.secondaryStyle + src + TextColor.RESET_ALL;
    }

    public String highlight(String src) {
        return this.hlStyle + src + TextColor.RESET_ALL;
    }

    public String highlightSubstring(String src, int start, int end) {
        String prefix = src.substring(0, start);
        String hlRegion = src.substring(start, end);
        String suffix = src.substring(end);

        StringBuilder combined = new StringBuilder();

        if (!prefix.isEmpty())
            combined.append(this.primary(prefix));
        if (!hlRegion.isEmpty())
            combined.append(this.highlight(hlRegion));
        if (!suffix.isEmpty())
            combined.append(this.primary(suffix));

        return combined.toString();
    }

    public String primaryLine(int lineNum, String line) {
        return this.linePrefix.apply(lineNum) + this.primary(line);
    }

    public String secondaryLine(int lineNum, String line) {
        return this.linePrefix.apply(lineNum) + this.secondary(line);
    }

    public String highlightLine(int lineNum, String line) {
        return this.linePrefix.apply(lineNum)
                + this.hlStyle
                + line
                + TextColor.RESET_ALL;
    }

    private List<String> lines(String src, BiFunction<Integer, String, String> styler) {
        String[] lines = NEWLINE_PATTERN.split(src);
        List<String> styled = new ArrayList<>(lines.length);

        for (int i = 0; i < lines.length; i++)
            styled.add(styler.apply(i, lines[i]));

        return styled;
    }

    public List<String> primaryLines(String src) {
        return this.lines(src, this::primaryLine);
    }

    public List<String> secondaryLines(String src) {
        return this.lines(src, this::secondaryLine);
    }

    public List<String> highlightLines(String src) {
        return this.lines(src, this::highlightLine);
    }

    public List<String> highlightSubstringLines(String src, int start, int end) {
        List<String> fmt = new LinkedList<>();

        int lastLinePos = 0;
        int line = 0;
        boolean foundStart = false;

        Matcher srcLineBreak = NEWLINE_PATTERN.matcher(src);
        while (srcLineBreak.find()) {
            if (!foundStart && srcLineBreak.start() >= start) {
                // The lastLinePos is the beginning of the line containing the start
                foundStart = true;
                StringBuilder hlLine = new StringBuilder();

                // Append the start of the line, not highlighted
                hlLine.append(this.primaryLine(line, src.substring(lastLinePos, start)));

                // Append the highlighted portion
                hlLine.append(this.highlight(src.substring(start, Math.min(srcLineBreak.start(), end))));

                // If the line contains the end of the highlight then finish off
                // the highlighting and finish.
                if (end <= srcLineBreak.start())
                    hlLine.append(this.primary(src.substring(end, srcLineBreak.start())));

                fmt.add(hlLine.toString());
            } else if (foundStart && srcLineBreak.start() <= end) {
                // This entire line should be highlighted
                fmt.add(this.highlightLine(line, src.substring(lastLinePos, srcLineBreak.start())));
            } else if (foundStart && end <= srcLineBreak.start()) {
                // This is the last line containing the highlighted region
                fmt.add(this.highlightLine(line, src.substring(lastLinePos, end))
                        + this.primary(src.substring(end, srcLineBreak.start())));
            }

            lastLinePos = srcLineBreak.end();
            line++;

            // If we are at the end of the hl region we are done
            if (end <= srcLineBreak.start()) break;
        }

        return fmt;
    }
}
