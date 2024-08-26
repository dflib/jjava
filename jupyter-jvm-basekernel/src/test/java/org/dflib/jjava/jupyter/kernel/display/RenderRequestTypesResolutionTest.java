package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RenderRequestTypesResolutionTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { "image/svg+xml", "image/svg+xml", Collections.singletonList("image/svg+xml") },
                { "image/svg+xml", "image/svg", Collections.singletonList("image/svg") },
                { "image/svg+xml", "image/svg+xml", Collections.singletonList("image/*") },
                { "image/svg+xml", "image/svg+xml", Collections.singletonList("image") },
                { "image/svg+xml", "application/xml", Collections.singletonList("application/xml") },
                { "image/svg+xml", "application/xml", Collections.singletonList("application/*") },
                { "image/svg+xml", "application/xml", Collections.singletonList("application") },
                { "image/svg+xml", "image/svg+xml", Collections.singletonList("*") },

                { "image/svg", "image/svg", Collections.singletonList("image/svg") },

                { "image/svg", null, Collections.singletonList("application/xml") },
                { "image/svg+xml", null, Collections.singletonList("application/json") },
        });
    }

    private final MIMEType supported;
    private final MIMEType expected;
    private final RenderRequestTypes requestTypes;

    public RenderRequestTypesResolutionTest(String supported, String expected, List<String> requestTypes) {
        this.supported = supported == null ? null : MIMEType.parse(supported);
        this.expected = expected == null ? null : MIMEType.parse(expected);

        RenderRequestTypes.Builder builder = new RenderRequestTypes.Builder(group -> {
            switch (group) {
                case "xml":
                    return MIMEType.APPLICATION_XML;
                case "json":
                    return MIMEType.APPLICATION_JSON;
                default:
                    return null;
            }
        });
        requestTypes.stream()
                .map(MIMEType::parse)
                .forEach(builder::withType);
        this.requestTypes = builder.build();
    }

    @Test
    public void test() {
        MIMEType actual = this.requestTypes.resolveSupportedType(this.supported);

        assertEquals(expected, actual);
    }
}