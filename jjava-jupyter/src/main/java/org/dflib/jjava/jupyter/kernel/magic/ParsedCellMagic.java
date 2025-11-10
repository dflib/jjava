package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

public class ParsedCellMagic {

    public final String name;
    public final List<String> args;
    public final String cellBodyAfterMagic;

    public ParsedCellMagic(String name, List<String> args, String cellBodyAfterMagic) {
        this.name = name;
        this.args = args;
        this.cellBodyAfterMagic = cellBodyAfterMagic;
    }
}
