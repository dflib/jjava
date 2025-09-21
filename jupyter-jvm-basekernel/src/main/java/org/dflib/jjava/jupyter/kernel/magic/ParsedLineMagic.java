package org.dflib.jjava.jupyter.kernel.magic;

public class ParsedLineMagic {

    public final LineMagicArgs magicCall;
    public final String raw;
    public final String rawCell;
    public final String linePrefix;

    public ParsedLineMagic(LineMagicArgs magicCall, String raw, String rawCell, String linePrefix) {
        this.magicCall = magicCall;
        this.raw = raw;
        this.rawCell = rawCell;
        this.linePrefix = linePrefix;
    }
}
