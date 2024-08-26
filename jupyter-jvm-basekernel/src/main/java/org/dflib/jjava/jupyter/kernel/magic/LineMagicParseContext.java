package org.dflib.jjava.jupyter.kernel.magic;

public interface LineMagicParseContext {
    public static LineMagicParseContext of(LineMagicArgs args, String raw, String rawCell, String rawContextPrefix) {
        return new LineMagicParseContext() {
            @Override
            public LineMagicArgs getMagicCall() {
                return args;
            }

            @Override
            public String getRaw() {
                return raw;
            }

            @Override
            public String getRawCell() {
                return rawCell;
            }

            @Override
            public String getRawContextPrefix() {
                return rawContextPrefix;
            }
        };
    }

    public LineMagicArgs getMagicCall();

    public String getRaw();

    public String getRawCell();

    public String getRawContextPrefix();

    public default String getLinePrefix() {
        String cellPrefix = getRawContextPrefix();
        return cellPrefix.substring(cellPrefix.lastIndexOf('\n') + 1);
    }

    public default String getEntireLine() {
        return getLinePrefix() + getRaw();
    }
}
