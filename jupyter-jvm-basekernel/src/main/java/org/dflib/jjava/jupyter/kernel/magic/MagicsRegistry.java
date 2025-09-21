package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable registry of Jupyter "magics" known to the kernel.
 */
public class MagicsRegistry {

    private final Map<String, LineMagic<?>> lineMagics;
    private final Map<String, CellMagic<?>> cellMagics;

    public MagicsRegistry(Map<String, LineMagic<?>> lineMagics, Map<String, CellMagic<?>> cellMagics) {
        this.lineMagics = lineMagics;
        this.cellMagics = cellMagics;
    }

    public <T> T evalLineMagic(String name, List<String> args) throws Exception {
        LineMagic<T> magic = (LineMagic<T>) lineMagics.get(name);

        if (magic == null) {
            throw new UndefinedMagicException(name, true);
        }

        return magic.execute(args);
    }

    public <T> T evalCellMagic(String name, List<String> args, String body) throws Exception {
        CellMagic<T> magic = (CellMagic<T>) cellMagics.get(name);

        if (magic == null) {
            throw new UndefinedMagicException(name, false);
        }

        return magic.execute(args, body);
    }

    public Set<String> getCellMagicNames() {
        return cellMagics.keySet();
    }

    public Set<String> getLineMagicNames() {
        return lineMagics.keySet();
    }
}
