package org.dflib.jjava.jupyter.kernel.magic;

import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable registry of Jupyter "magics" known to the kernel.
 */
public class MagicsRegistry {

    private final Map<String, LineMagic<?, ?>> lineMagics;
    private final Map<String, CellMagic<?, ?>> cellMagics;

    public MagicsRegistry(Map<String, LineMagic<?, ?>> lineMagics, Map<String, CellMagic<?, ?>> cellMagics) {
        this.lineMagics = lineMagics;
        this.cellMagics = cellMagics;
    }

    public <T, K extends BaseKernel> T evalLineMagic(K kernel, String name, List<String> args) throws Exception {
        LineMagic<T, K> magic = (LineMagic<T, K>) lineMagics.get(name);

        if (magic == null) {
            throw new UndefinedMagicException(name, true);
        }

        return magic.eval(kernel, args);
    }

    public <T, K extends BaseKernel> T evalCellMagic(K kernel, String name, List<String> args, String body) throws Exception {
        CellMagic<T, K> magic = (CellMagic<T, K>) cellMagics.get(name);

        if (magic == null) {
            throw new UndefinedMagicException(name, false);
        }

        return magic.eval(kernel, args, body);
    }

    public Set<String> getCellMagicNames() {
        return cellMagics.keySet();
    }

    public Set<String> getLineMagicNames() {
        return lineMagics.keySet();
    }
}
