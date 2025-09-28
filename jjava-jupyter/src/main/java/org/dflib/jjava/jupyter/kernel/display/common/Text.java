package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

public class Text {
    public static MIMEType JS = MIMEType.APPLICATION_JAVASCRIPT;
    public static MIMEType PLAIN = MIMEType.TEXT_PLAIN;
    public static MIMEType MARKDOWN = MIMEType.TEXT_MARKDOWN;
    public static MIMEType LATEX = MIMEType.TEXT_LATEX;
    public static MIMEType HTML = MIMEType.TEXT_HTML;
    public static MIMEType CSS = MIMEType.TEXT_CSS;
    public static MIMEType SVG = MIMEType.IMAGE_SVG;
    public static MIMEType JSON = MIMEType.APPLICATION_JSON;

    public static void registerAll(Renderer renderer) {
        renderer.createRegistration(CharSequence.class)
                .preferring(PLAIN)
                .supporting(JS, MARKDOWN, LATEX, HTML, CSS, SVG)
                .register(Text::renderCharSequence);
    }

    public static void renderCharSequence(CharSequence data, RenderContext context) {
        context.renderIfRequested(JS, () -> data);
        context.renderIfRequested(PLAIN, () -> data);
        context.renderIfRequested(MARKDOWN, () -> data);
        context.renderIfRequested(LATEX, () -> data);
        context.renderIfRequested(HTML, () -> data);
        context.renderIfRequested(CSS, () -> data);
        context.renderIfRequested(SVG, () -> data);
        context.renderIfRequested(JSON, () -> data);
    }
}
