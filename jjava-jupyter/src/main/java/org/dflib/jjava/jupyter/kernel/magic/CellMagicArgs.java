package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

public class CellMagicArgs extends LineMagicArgs {

    public final String body;

    public CellMagicArgs(String name, List<String> args, String body) {
        super(name, args);
        this.body = body;
    }
}
