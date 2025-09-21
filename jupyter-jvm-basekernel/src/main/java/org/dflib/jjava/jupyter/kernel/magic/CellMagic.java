package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

/**
 * Defines contract for a custom "line magic".
 * @param <T>
 */
@FunctionalInterface
public interface CellMagic<T> {
    
    T execute(List<String> args, String body) throws Exception;
}
