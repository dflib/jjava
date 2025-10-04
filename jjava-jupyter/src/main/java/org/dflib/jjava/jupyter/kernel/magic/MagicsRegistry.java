package org.dflib.jjava.jupyter.kernel.magic;

import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry of Jupyter "magics" known to the kernel. Magics can be registered or redefined by the kernel bootstrap
 * code as well as notebook code and (eventually?) extensions.
 */
public class MagicsRegistry {

    private final Map<String, LineMagic<?, ?>> lineMagics;
    private final Map<String, CellMagic<?, ?>> cellMagics;

    public MagicsRegistry() {
        this.lineMagics = new ConcurrentHashMap<>();
        this.cellMagics = new ConcurrentHashMap<>();
    }

    /**
     * Registers or overrides a named LineMagic
     */
    public MagicsRegistry registerLineMagic(String name, LineMagic<?, ?> magic) {
        lineMagics.put(name, magic);
        return this;
    }

    /**
     * Registers or overrides a named CellMagic
     */
    public MagicsRegistry registerCellMagic(String name, CellMagic<?, ?> magic) {
        cellMagics.put(name, magic);
        return this;
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
