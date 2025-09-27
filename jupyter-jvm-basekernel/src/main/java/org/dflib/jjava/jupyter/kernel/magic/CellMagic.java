package org.dflib.jjava.jupyter.kernel.magic;

import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.util.List;

/**
 * Defines the contract for a custom "line magic".
 */
@FunctionalInterface
public interface CellMagic<T, K extends BaseKernel> {

    T execute(K kernel, List<String> args, String body) throws Exception;
}
