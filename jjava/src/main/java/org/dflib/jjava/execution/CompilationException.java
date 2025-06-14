/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Spencer Park
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.dflib.jjava.execution;

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
