package org.dflib.jjava.jupyter.kernel.display.mime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MIMETypeTest {
    @Parameters(name = "{index}: MIMEType.parse({0}) = new MIMEType({1}, {2}, {3}, {4})")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { "application/json", "application", null, "json", null },
                { "application/xhtml+xml", "application", null, "xhtml", "xml" },
                { "image/*", "image", null, "*", null },
                { "image/", "image", null, "", null },
                { "video", "video", null, null, null },
                { "video", "video", null, null, null },
                { "application/vnd.media", "application", "vnd", "media", null },
                { "application/vnd.media.producer", "application", "vnd", "media.producer", null },
                { "application/vnd.media.producer+suffix", "application", "vnd", "media.producer", "suffix" },
                { "application/vnd.media.named+producer+suffix", "application", "vnd", "media.named+producer", "suffix" },
        });
    }

    private final String raw;
    private final String type;
    private final String tree;
    private final String subtype;
    private final String suffix;

    public MIMETypeTest(String raw, String type, String tree, String subtype, String suffix) {
        this.raw = raw;
        this.type = type;
        this.tree = tree;
        this.subtype = subtype;
        this.suffix = suffix;
    }

    @Test
    public void test() {
        MIMEType parsed = MIMEType.parse(this.raw);
        MIMEType expected = new MIMEType(this.type, this.tree, this.subtype, this.suffix);

        assertEquals(expected, parsed);
    }
}