package org.dflib.jjava.jupyter.kernel.magic.registry;

import java.util.List;

@FunctionalInterface
public interface CellMagicFunction<T> {
    public T execute(List<String> args, String body) throws Exception;
}
