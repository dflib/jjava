package org.dflib.jjava.distro;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Everything a single notebook cell sent back over the Jupyter protocol.
 */
public class CellOutput {

    private final int number;
    private final String source;
    private final Map<String, String> fields;

    CellOutput(int number, String source, Map<String, String> fields) {
        this.number = number;
        this.source = source;
        this.fields = fields;
    }

    public int number() {
        return number;
    }

    public String source() {
        return source;
    }

    /**
     * Either "ok" or "error" - the status of the "execute_reply" message.
     */
    public String status() {
        return field("status");
    }

    public boolean isOk() {
        return "ok".equals(status());
    }

    /**
     * The "text/plain" rendering of the cell value, empty if the cell produced no value.
     */
    public String result() {
        return field("result");
    }

    /**
     * The "text/plain" rendering of the cell "display_data", empty if there was none.
     */
    public String display() {
        return field("display");
    }

    public String stdout() {
        return field("stdout");
    }

    public String stderr() {
        return field("stderr");
    }

    /**
     * The error reported for the cell, with terminal colors stripped. Starts with
     * "&lt;exception name&gt;: &lt;exception message&gt;", followed by the traceback, which for
     * compilation failures is the JShell diagnostic rendering. Empty if the cell succeeded.
     */
    public String error() {
        return field("error");
    }

    /**
     * Asserts that the cell failed, and that its error contains every one of the provided fragments.
     */
    public CellOutput assertError(String... errorFragments) {
        if (isOk()) {
            fail("Cell " + number + " was expected to fail, but did not:\n" + this);
        }

        String error = error();
        for (String fragment : errorFragments) {
            if (!error.contains(fragment)) {
                fail("Cell " + number + " error does not contain '" + fragment + "':\n" + this);
            }
        }

        return this;
    }

    String field(String name) {
        return fields.getOrDefault(name, "");
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("--- In [").append(number).append("] ---\n").append(source).append('\n');
        appendField(out, "status", status());
        appendField(out, "result", result());
        appendField(out, "display", display());
        appendField(out, "stdout", stdout());
        appendField(out, "stderr", stderr());
        appendField(out, "error", error());
        return out.toString();
    }

    private void appendField(StringBuilder out, String name, String value) {
        if (!value.isEmpty()) {
            out.append("  [").append(name).append("] ").append(value.replace("\n", "\n    ")).append('\n');
        }
    }
}
