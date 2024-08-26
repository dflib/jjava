package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

public interface CellMagicArgs extends LineMagicArgs {
    public static CellMagicArgs of(String name, List<String> args, String body) {
        return new CellMagicArgs() {
            @Override
            public String getBody() {
                return body;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<String> getArgs() {
                return args;
            }
        };
    }

    public String getBody();
}
