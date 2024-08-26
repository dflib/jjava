package org.dflib.jjava.jupyter.kernel.history;

public class HistoryEntry {
    protected final int session;

    protected final int cellNumber;

    protected final String input;

    /**
     * null if output was specified as false in the request
     */
    protected final String output;

    public HistoryEntry(int session, int cellNumber, String input) {
        this.session = session;
        this.cellNumber = cellNumber;
        this.input = input;
        this.output = null;
    }

    public HistoryEntry(int session, int cellNumber, String input, String output) {
        this.session = session;
        this.cellNumber = cellNumber;
        this.input = input;
        this.output = output;
    }

    public int getSession() {
        return session;
    }

    public int getCellNumber() {
        return cellNumber;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public boolean hasOutput() {
        return output != null;
    }
}
