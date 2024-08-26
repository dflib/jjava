package org.dflib.jjava.jupyter.kernel.display.mime;

public class MIMETypeParseException extends RuntimeException {
    private final String raw;
    private final int position;
    private final String problem;

    public MIMETypeParseException(String raw, int position, String problem) {
        super(raw + '@' + position + ": " + problem);
        this.raw = raw;
        this.position = position;
        this.problem = problem;
    }

    public MIMETypeParseException(String raw, int position, String problem, Throwable cause) {
        super(raw + '@' + position + ": " + problem, cause);
        this.raw = raw;
        this.position = position;
        this.problem = problem;
    }

    public String getSource() {
        return raw;
    }

    public int getPosition() {
        return position;
    }

    public String getProblem() {
        return problem;
    }
}
