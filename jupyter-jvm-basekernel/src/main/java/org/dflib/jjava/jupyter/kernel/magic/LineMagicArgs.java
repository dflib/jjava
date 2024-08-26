package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

public interface LineMagicArgs {
    public static LineMagicArgs of(String name, List<String> args) {
        return new LineMagicArgs() {
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

    public String getName();

    public List<String> getArgs();
}
