package org.dflib.jjava.jupyter.kernel.display.mime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MIMETypeTest {

    @ParameterizedTest(name = "{index}: MIMEType.parse({0}) = new MIMEType({1}, {2}, {3}, {4})")
    @CsvSource({
            "application/json,                              application,    ,       json,                   ",
            "application/xhtml+xml,                         application,    ,       xhtml,                  xml",
            "image/*,                                       image,          ,       *,                      ",
            "image/,                                        image,          ,       '',                     ",
            "video,                                         video,          ,       ,                       ",
            "video,                                         video,          ,       ,                       ",
            "application/vnd.media,                         application,    vnd,    media,                  ",
            "application/vnd.media.producer,                application,    vnd,    media.producer,         ",
            "application/vnd.media.producer+suffix,         application,    vnd,    media.producer,         suffix",
            "application/vnd.media.named+producer+suffix,   application,    vnd,    media.named+producer,   suffix"
    })
    public void test(String raw, String type, String tree, String subtype, String suffix) {
        MIMEType expected = new MIMEType(type, tree, subtype, suffix);
        MIMEType actual = MIMEType.parse(raw);

        assertEquals(expected, actual);
    }
}
