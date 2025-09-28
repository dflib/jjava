package org.dflib.jjava.jupyter.kernel;

import java.util.List;

public class ReplacementOptions {
    private final List<String> replacements;

    private final int sourceStart;
    private final int sourceEnd;

    public ReplacementOptions(List<String> replacements, int sourceStart, int sourceEnd) {
        this.replacements = replacements;
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
    }

    public List<String> getReplacements() {
        return replacements;
    }

    public int getSourceStart() {
        return sourceStart;
    }

    public int getSourceEnd() {
        return sourceEnd;
    }
}
