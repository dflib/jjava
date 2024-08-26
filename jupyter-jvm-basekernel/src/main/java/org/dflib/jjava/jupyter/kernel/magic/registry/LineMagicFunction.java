package org.dflib.jjava.jupyter.kernel.magic.registry;

import java.util.List;

@FunctionalInterface
public interface LineMagicFunction<T> {
    public T execute(List<String> args) throws Exception;
}
