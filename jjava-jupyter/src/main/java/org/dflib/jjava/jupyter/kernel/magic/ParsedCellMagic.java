package org.dflib.jjava.jupyter.kernel.magic;

public class ParsedCellMagic {

    public final CellMagicArgs magicCall;
    public final String rawArgsLine;
    public final String rawCell;

    public ParsedCellMagic(CellMagicArgs magicCall, String rawArgsLine, String rawCell) {
        this.magicCall = magicCall;
        this.rawArgsLine = rawArgsLine;
        this.rawCell = rawCell;
    }
}
