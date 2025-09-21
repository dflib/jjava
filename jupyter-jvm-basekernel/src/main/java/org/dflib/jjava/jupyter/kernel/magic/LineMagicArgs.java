package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

public class LineMagicArgs {

    public final String name;
    public final List<String> args;

    public LineMagicArgs(String name, List<String> args) {
        this.args = args;
        this.name = name;
    }
}
