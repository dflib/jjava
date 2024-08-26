package org.dflib.jjava.jupyter.kernel.magic.registry;

import java.util.List;

public class StaticMagics {
    @LineMagic
    public static int staticMagic(List<String> args) {
        return args.size();
    }

    @CellMagic("staticMagic")
    public static String staticCellMagic(List<String> args, String body) {
        return body;
    }
}
