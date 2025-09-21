package org.dflib.jjava.jupyter.kernel.magic;

/**
 * A converter ("transpiler") of the generic magic syntax into kernel-specific syntax (such as Java).
 */
public interface MagicTranspiler {

    String transpileCell(ParsedCellMagic magic);

    String transpileLine(ParsedLineMagic magic);
}
