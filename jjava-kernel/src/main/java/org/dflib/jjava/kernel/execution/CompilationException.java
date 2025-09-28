package org.dflib.jjava.kernel.execution;

import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

public class CompilationException extends RuntimeException {
    private final SnippetEvent badSnippetCompilation;

    static String toMessage(SnippetEvent e) {
        String source = e.snippet().source() != null ? e.snippet().source().trim() : null;
        StringBuilder out = new StringBuilder("[snippet: [").append(source).append("]");

        if (e.causeSnippet() != null) {
            String causeSource = e.causeSnippet().source() != null ? e.causeSnippet().source().trim() : null;
            out.append(", causeSnippet: [").append(causeSource).append("]");
        }

        if (e.status() != null) {
            out.append(", status: ").append(e.status());
        }

        if (e.previousStatus() != null && e.previousStatus() != Snippet.Status.NONEXISTENT) {
            out.append(", previousStatus: ").append(e.previousStatus());
        }

        if (e.isSignatureChange()) {
            out.append(",i sSignatureChange: true");
        }

        if (e.value() != null) {
            out.append(", value: ").append(e.value());
        }

        return out.append("]").toString();
    }

    public CompilationException(SnippetEvent badSnippetCompilation) {
        super(
                toMessage(badSnippetCompilation),
                badSnippetCompilation.exception()
        );

        this.badSnippetCompilation = badSnippetCompilation;
    }

    public SnippetEvent getBadSnippetCompilation() {
        return badSnippetCompilation;
    }
}
