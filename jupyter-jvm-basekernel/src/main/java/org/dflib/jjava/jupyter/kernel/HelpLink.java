package org.dflib.jjava.jupyter.kernel;

public class HelpLink {

    private final String text;
    private final String url;

    public HelpLink(String text, String url) {
        this.text = text;
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }
}
