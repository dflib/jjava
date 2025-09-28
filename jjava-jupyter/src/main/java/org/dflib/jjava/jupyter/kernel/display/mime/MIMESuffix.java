package org.dflib.jjava.jupyter.kernel.display.mime;

/**
 * <a href="https://tools.ietf.org/html/rfc6839">RFC 6839</a> for the
 * +xml, +json, +ber, +der, +fastinfoset, +wbxml, +zip
 * <p>
 * <a href="https://tools.ietf.org/html/rfc7049">RFC 7049</a> for the
 * +cbor
 */
public class MIMESuffix {
    public static final MIMESuffix XML = new MIMESuffix("xml", MIMEType.APPLICATION_XML);
    public static final MIMESuffix JSON = new MIMESuffix("json", MIMEType.APPLICATION_JSON);
    public static final MIMESuffix BER = new MIMESuffix("ber", null);
    public static final MIMESuffix DER = new MIMESuffix("der", null);
    public static final MIMESuffix FASTINFOSET = new MIMESuffix("fastinfoset", MIMEType.APPLICATION_FASTINFOSET);
    public static final MIMESuffix WBXML = new MIMESuffix("wbxml", MIMEType.APPLICATION_VND_WAP_WBXML);
    public static final MIMESuffix ZIP = new MIMESuffix("zip", MIMEType.APPLICATION_ZIP);
    public static final MIMESuffix CBOR = new MIMESuffix("cbor", MIMEType.APPLICATION_CBOR);

    public static MIMESuffix of(String name) {
        if (name == null) return null;
        switch (name.toLowerCase()) {
            case "xml":
                return XML;
            case "json":
                return JSON;
            case "ber":
                return BER;
            case "der":
                return DER;
            case "fastinfoset":
                return FASTINFOSET;
            case "wbxml":
                return WBXML;
            case "zip":
                return ZIP;
            case "cbor":
                return CBOR;
            default:
                return new MIMESuffix(name.toLowerCase(), null);
        }
    }

    public static MIMESuffix of(MIMEType type) {
        return MIMESuffix.of(type.getSuffix());
    }

    private final String suffix;
    private final MIMEType delegate;

    private MIMESuffix(String suffix, MIMEType delegate) {
        this.suffix = suffix;
        this.delegate = delegate;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public MIMEType getDelegate() {
        return this.delegate;
    }

    public boolean hasDelegate() {
        return this.delegate != null;
    }
}
