package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

public class Url {
    public static String EMBED_KEY = "embed";
    public static String HTML_TAG_KEY = "url.html.tag";
    public static String HTML_SRC_ATTR_KEY = "url.html.src-attr";

    public static void registerAll(Renderer renderer) {
        renderer.createRegistration(java.net.URL.class)
                .supporting(MIMEType.ANY)
                .register(Url::renderUrl);
        renderer.createRegistration(java.net.URLConnection.class)
                .supporting(MIMEType.ANY)
                .register((conn, ctx) -> renderUrl(conn.getURL(), ctx));
    }

    public static void renderUrl(java.net.URL url, RenderContext context) {
        if (context.getParameterAsBoolean(EMBED_KEY, false)) {
            try {
                Object content = url.getContent();
                DisplayData rendered = context.getRenderer().render(content, context.getParams());
                context.getOutputContainer().assign(rendered);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            context.renderIfRequested(MIMEType.TEXT_HTML, () -> {
                String tag = context.getParameterAsString(HTML_TAG_KEY, "a");
                String srcAttr = context.getParameterAsString(HTML_SRC_ATTR_KEY, "src");
                return renderHTML(tag, srcAttr, url, Collections.emptyMap());
            });
        }
    }

    private static String renderHTML(String tag, String srcAttr, java.net.URL url, Map<String, String> attrs) {
        String encodedUrl;
        try {
            encodedUrl = URLEncoder.encode(url.toExternalForm(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Should never happen...
        }

        //TODO add some html rendering utilities for the url and html entity encoding
        StringBuilder html = new StringBuilder("<");
        html.append(tag);
        html.append(" ").append(srcAttr).append("=\"").append(encodedUrl).append('"');
        attrs.forEach((attr, val) -> {
            if (val != null)
                html.append(" ").append(attr).append("=\"").append(val).append("\"");
        });
        return html.toString();
    }
}
