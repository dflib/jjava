package org.dflib.jjava.jupyter.kernel.display.mime;

import org.dflib.jjava.jupyter.kernel.util.CharPredicate;

import java.util.Locale;
import java.util.Objects;

public class MIMEType {
    //TODO look into caching parsed strings in a weakmap?

    private static final CharPredicate RESTRICTED_NAME_CHAR = CharPredicate.builder()
            .inRange('a', 'z')
            .inRange('A', 'Z')
            .inRange('0', '9')
            .match("!#$&-^_")
            .build();

    private static final String WILDCARD = "*";

    public static final MIMEType ANY = new MIMEType(WILDCARD, null, WILDCARD, null);

    public static final MIMEType APPLICATION_XML = MIMEType.parse("application/xml");
    public static final MIMEType APPLICATION_JSON = MIMEType.parse("application/json");
    public static final MIMEType APPLICATION_JAVASCRIPT = MIMEType.parse("application/javascript");
    public static final MIMEType APPLICATION_PDF = MIMEType.parse("application/pdf");

    public static final MIMEType APPLICATION_FASTINFOSET = MIMEType.parse("application/fastinfoset");
    public static final MIMEType APPLICATION_VND_WAP_WBXML = MIMEType.parse("application/vnd.wap.wbxml");
    public static final MIMEType APPLICATION_ZIP = MIMEType.parse("application/zip");
    /**
     * There is a cbor {@code <->} json conversion that can happen.
     */
    public static final MIMEType APPLICATION_CBOR = MIMEType.parse("application/cbor");

    public static final MIMEType TEXT_HTML = MIMEType.parse("text/html");
    public static final MIMEType TEXT_MARKDOWN = MIMEType.parse("text/markdown");
    public static final MIMEType TEXT_LATEX = MIMEType.parse("text/latex");
    public static final MIMEType TEXT_PLAIN = MIMEType.parse("text/plain");
    public static final MIMEType TEXT_CSS = MIMEType.parse("text/css");

    public static final MIMEType IMAGE_PNG = MIMEType.parse("image/png");
    public static final MIMEType IMAGE_JPEG = MIMEType.parse("image/jpeg");
    public static final MIMEType IMAGE_GIF = MIMEType.parse("image/gif");
    public static final MIMEType IMAGE_SVG = MIMEType.parse("image/svg+xml");

    /**
     * Construct a {@link MIMEType} from a string representation. The grammar
     * is from <a href="https://tools.ietf.org/html/rfc6838">RFC 6838 Section 4.2</a>.
     * <pre>
     *     type-name = restricted-name
     *     subtype-name = restricted-name
     *
     *     restricted-name = restricted-name-first *126restricted-name-chars
     *     restricted-name-first  = ALPHA / DIGIT
     *     restricted-name-chars  = ALPHA / DIGIT / "!" / "#" /
     *                              "$" / "&amp;" / "-" / "^" / "_"
     *     restricted-name-chars =/ "." ; Characters before first dot always
     *                                  ; specify a facet name
     *     restricted-name-chars =/ "+" ; Characters after last plus always
     *                                  ; specify a structured syntax suffix
     * </pre>
     * The parser makes some modifications to the specification:
     * <ol>
     * <li>No length restriction on the segments</li>
     * <li>A subtype may also match exactly <code>"*"</code></li>
     * </ol>
     *
     * @param raw the MIME type represented by a string
     *
     * @return the {@link MIMEType} represented by the string
     *
     * @throws MIMETypeParseException if the string representation doesn't match
     *                                the specification
     */
    public static MIMEType parse(String raw) throws MIMETypeParseException {
        if (WILDCARD.equals(raw))
            return ANY;

        String type = null;
        String tree = null;
        String subtype;
        String suffix = null;

        int subtypeStart = 0;
        int pos = -1;

        while (++pos < raw.length()) {
            char c = raw.charAt(pos);
            switch (c) {
                case '+':
                case '.':
                    continue;
            }
            if (RESTRICTED_NAME_CHAR.test(c))
                continue;
            if (c != '/')
                throw new MIMETypeParseException(raw, pos, String.format("Expected '/' but got %c", c));
            type = raw.substring(0, pos);
            subtypeStart = pos + 1;
            break;
        }

        if (pos == raw.length()) {
            return new MIMEType(raw, null, null, null);
        } else if (subtypeStart + 1 == raw.length() && raw.charAt(pos + 1) == '*') {
            return new MIMEType(type, null, WILDCARD, null);
        }

        int lastSuffixStartPos = -1;
        while (++pos < raw.length()) {
            char c = raw.charAt(pos);
            switch (c) {
                case '.':
                    if (tree == null) {
                        tree = raw.substring(subtypeStart, pos);
                        subtypeStart = pos + 1;
                    }
                    continue;
                case '+':
                    lastSuffixStartPos = pos;
                    continue;
            }
            if (RESTRICTED_NAME_CHAR.test(c))
                continue;

            throw new MIMETypeParseException(raw, pos, String.format("Unexpected char '%c'", c));
        }

        if (lastSuffixStartPos != -1) {
            subtype = raw.substring(subtypeStart, lastSuffixStartPos);
            suffix = raw.substring(lastSuffixStartPos + 1);
        } else {
            subtype = raw.substring(subtypeStart);
        }

        return new MIMEType(type, tree, subtype, suffix);
    }

    private final String group;
    private final String tree;
    private final String subtype;
    private final String suffix;

    public MIMEType(String group, String tree, String subtype, String suffix) {
        if (group == null)
            throw new IllegalArgumentException("Group must be given.");

        this.group = group.toLowerCase(Locale.ENGLISH);
        this.tree = tree != null ? tree.toLowerCase(Locale.ENGLISH) : null;
        this.subtype = subtype != null ? subtype.toLowerCase(Locale.ENGLISH) : null;
        this.suffix = suffix != null ? suffix.toLowerCase(Locale.ENGLISH) : null;
    }

    public String getGroup() {
        return group;
    }

    public String getTree() {
        return tree;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean hasTree() {
        return this.tree != null;
    }

    public boolean hasSubtype() {
        return this.subtype != null;
    }

    public boolean hasSuffix() {
        return this.suffix != null;
    }

    public MIMEType withoutSuffix() {
        return !this.hasSuffix()
                ? this
                : new MIMEType(this.group, this.tree, this.subtype, null);
    }

    public boolean subtypeIsWildcard() {
        return WILDCARD.equals(this.subtype);
    }

    public boolean isWildcard() {
        return WILDCARD.equals(this.group);
    }

    public boolean groupEquals(String group) {
        return this.getGroup().equalsIgnoreCase(group);
    }

    public boolean treeEquals(String tree) {
        return this.hasTree()
                ? this.getTree().equalsIgnoreCase(tree)
                : tree == null;
    }

    public boolean subtypeEquals(String subtype) {
        return this.hasSubtype()
                ? this.getSubtype().equalsIgnoreCase(subtype)
                : subtype == null;
    }

    public boolean suffixEquals(String suffix) {
        return this.hasSuffix()
                ? this.getSuffix().equalsIgnoreCase(suffix)
                : suffix == null;
    }

    public boolean hasSameGroupAs(MIMEType other) {
        return this.getGroup().equals(other.getGroup());
    }

    public boolean hasSameTreeAs(MIMEType other) {
        return Objects.equals(this.getTree(), other.getTree());
    }

    public boolean hasSameSubtypeAs(MIMEType other) {
        return Objects.equals(this.getSubtype(), other.getSubtype());
    }

    public boolean hasSameSuffixAs(MIMEType other) {
        return Objects.equals(this.getSuffix(), other.getSubtype());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MIMEType mimeType = (MIMEType) o;
        return Objects.equals(group, mimeType.group) &&
                Objects.equals(tree, mimeType.tree) &&
                Objects.equals(subtype, mimeType.subtype) &&
                Objects.equals(suffix, mimeType.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, tree, subtype, suffix);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getGroup());
        if (hasSubtype()) sb.append('/');
        if (hasTree()) sb.append(getTree()).append('.');
        if (hasSubtype()) sb.append(getSubtype());
        if (hasSuffix()) sb.append('+').append(getSuffix());
        return sb.toString();
    }
}
