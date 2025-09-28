package org.dflib.jjava.jupyter.kernel.magic;

public class UndefinedMagicException extends RuntimeException {
    private final String name;
    private final boolean line;

    public UndefinedMagicException(String name, boolean line) {
        super("Undefined " + (line ? "line" : "cell") + " magic '" + name + "'");
        this.name = name;
        this.line = line;
    }

    public String getMagicName() {
        return name;
    }

    public boolean isLineMagic() {
        return line;
    }

    public boolean isCellMagic() {
        return !line;
    }
}
