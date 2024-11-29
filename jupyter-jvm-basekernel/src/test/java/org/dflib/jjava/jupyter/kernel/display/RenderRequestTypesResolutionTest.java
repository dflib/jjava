package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RenderRequestTypesResolutionTest {

    @ParameterizedTest
    @MethodSource("data")
    public void test(String supported, String expected, List<String> requestTypes) {
        MIMEType supportedMime = MIMEType.parse(supported);
        MIMEType expectedMime = expected == null ? null : MIMEType.parse(expected);
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
        RenderRequestTypes renderRequestTypes = builder.build();

        MIMEType actualMime = renderRequestTypes.resolveSupportedType(supportedMime);

        assertEquals(expectedMime, actualMime);
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("image/svg+xml", "image/svg+xml", List.of("image/svg+xml")),
                Arguments.of("image/svg+xml", "image/svg", List.of("image/svg")),
                Arguments.of("image/svg+xml", "image/svg+xml", List.of("image/*")),
                Arguments.of("image/svg+xml", "image/svg+xml", List.of("image")),
                Arguments.of("image/svg+xml", "application/xml", List.of("application/xml")),
                Arguments.of("image/svg+xml", "application/xml", List.of("application/*")),
                Arguments.of("image/svg+xml", "application/xml", List.of("application")),
                Arguments.of("image/svg+xml", "image/svg+xml", List.of("*")),
                Arguments.of("image/svg", "image/svg", List.of("image/svg")),
                Arguments.of("image/svg", null, List.of("application/xml")),
                Arguments.of("image/svg+xml", null, List.of("application/json"))
        );
    }
}
