package org.dflib.jjava.jupyter.kernel.magic;

import java.util.List;

public class ParsedLineMagic {

    public final String name;
    public final List<String> args;
    public final String magicLinePrefix;
    public final String unparsedMagic;

    public ParsedLineMagic(String name, List<String> args, String magicLinePrefix, String unparsedMagic) {
        this.name = name;
        this.args = args;
        this.magicLinePrefix = magicLinePrefix;
        this.unparsedMagic = unparsedMagic;
    }
}
