package org.dflib.jjava.jupyter.kernel.magic;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MagicsArgsTest {

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void values(MagicsArgs schema, String args, Matcher<Map<String, List<String>>> matcher) {
        List<String> rawArgs = MagicsResolver.split(args);
        Map<String, List<String>> actual = assertDoesNotThrow(() -> schema.parse(rawArgs));

        assertThat(actual, matcher);
    }

    public static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(
                        args(b -> b.required("a")),
                        "value-a",
                        hasEntry("a", List.of("value-a"))
                ),
                Arguments.of(
                        args(b -> b.required("a").optional("b")),
                        "value-a",
                        allOf(hasEntry("a", List.of("value-a")), hasEntry("b", List.<String>of()))
                ),
                Arguments.of(
                        args(b -> b.required("a").optional("b")),
                        "value-a value-b",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.of("value-b"))
                        )
                ),
                Arguments.of(
                        args(b -> b.required("a").optional("b").varargs("c")),
                        "value-a value-b",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.of("value-b")),
                                hasEntry("c", List.<String>of())
                        )
                ),
                Arguments.of(args(b -> b.required("a").optional("b").varargs("c")),
                        "value-a value-b value-c",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.of("value-b")),
                                hasEntry("c", List.of("value-c"))
                        )
                ),
                Arguments.of(
                        args(b -> b.required("a").optional("b").varargs("c")),
                        "value-a value-b value-c-1 value-c-2",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.of("value-b")),
                                hasEntry("c", List.of("value-c-1", "value-c-2"))
                        )
                ),
                Arguments.of(
                        args(b -> b.required("a").required("b").varargs("c")),
                        "value-a value-b value-c-1 value-c-2",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.of("value-b")),
                                hasEntry("c", List.of("value-c-1", "value-c-2"))
                        )
                ),
                Arguments.of(
                        args(b -> b.optional("a")),
                        "",
                        hasEntry("a", List.of())
                ),
                Arguments.of(
                        args(b -> b.optional("a").varargs("b")),
                        "",
                        allOf(
                                hasEntry("a", List.of()),
                                hasEntry("b", List.of())
                        )
                ),
                Arguments.of(
                        args(b -> b.optional("a").varargs("b")),
                        "value-a",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.<String>of())
                        )
                ),
                Arguments.of(
                        args(b -> b.optional("a").varargs("b")),
                        "value-a",
                        allOf(
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.<String>of())
                        )
                ),
                Arguments.of(
                        args(b -> b.varargs("a")),
                        "",
                        hasEntry("a", List.of())
                ),
                Arguments.of(
                        args(b -> b.varargs("a")),
                        "value-a",
                        hasEntry("a", List.of("value-a"))
                ),
                Arguments.of(
                        args(b -> b.required("a").optional("a")),
                        "value-a extra-a",
                        hasEntry("a", List.of("value-a", "extra-a"))
                ),
                Arguments.of(
                        args(b -> b.required("a").optional("a")),
                        "value-a",
                        hasEntry("a", List.of("value-a"))
                ),
                Arguments.of(
                        args(b -> b.required("a").varargs("a")),
                        "value-a extra-a extra-a-2",
                        hasEntry("a", List.of("value-a", "extra-a", "extra-a-2"))
                )
        );
    }

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void flags(MagicsArgs schema, String args, Matcher<Map<String, List<String>>> matcher) {
        List<String> rawArgs = MagicsResolver.split(args);
        Map<String, List<String>> actual = Assertions.assertDoesNotThrow(() -> schema.parse(rawArgs));

        assertThat(actual, matcher);
    }

    public static Stream<Arguments> flags() {
        return Stream.of(
                Arguments.of(
                        args(b -> {}),
                        "-f",
                        hasEntry("f", List.of(""))
                ),
                Arguments.of(
                        args(b -> {}),
                        "-fff",
                        hasEntry("f", List.of("", "", ""))
                ),
                Arguments.of(
                        args(b -> {}),
                        "-fg -g",
                        allOf(hasEntry("f", List.of("")), hasEntry("g", List.of("", "")))
                ),
                Arguments.of(
                        args(b -> b.flag("test", 'f')),
                        "",
                        hasEntry("test", List.of())
                ),
                Arguments.of(
                        args(b -> b.flag("verbose", 'v', "true")),
                        "-v",
                        hasEntry("verbose", List.of("true"))
                )
        );
    }

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void keywords(MagicsArgs schema, String args, Matcher<Map<String, List<String>>> matcher) {
        List<String> rawArgs = MagicsResolver.split(args);
        Map<String, List<String>> actual = Assertions.assertDoesNotThrow(() -> schema.parse(rawArgs));

        assertThat(actual, matcher);
    }

    public static Stream<Arguments> keywords() {
        return Stream.of(
                Arguments.of(
                        args(b -> {}),
                        "--f=10",
                        hasEntry("f", List.of("10"))
                ),
                Arguments.of(
                        args(b -> {}),
                        "--f=10 --f=11",
                        hasEntry("f", List.of("10", "11"))
                ),
                Arguments.of(
                        args(b -> {}),
                        "--f 10 --f=11 --f 12",
                        hasEntry("f", List.of("10", "11", "12"))
                ),
                Arguments.of(
                        args(b -> b.keyword("test")),
                        "--test=10 --test 11 --test=12",
                        hasEntry("test", List.of("10", "11", "12"))
                ),
                Arguments.of(
                        args(b -> b.keyword("test", MagicsArgs.KeywordSpec.REPLACE)),
                        "--test=10 --test 11 --test=12",
                        hasEntry("test", List.of("12"))
                ),
                Arguments.of(
                        args(b -> b.keyword("test")),
                        "",
                        hasEntry("test", List.of())
                )
        );
    }

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void flagsAndKeyWords(MagicsArgs schema, String args, Matcher<Map<String, List<String>>> matcher) {
        List<String> rawArgs = MagicsResolver.split(args);
        Map<String, List<String>> actual = Assertions.assertDoesNotThrow(() -> schema.parse(rawArgs));

        assertThat(actual, matcher);
    }

    public static Stream<Arguments> flagsAndKeyWords() {
        return Stream.of(
                Arguments.of(
                        args(b -> b.flag("log-level", 'v', "100").keyword("log-level")),
                        "-v --log-level=200 --log-level 300",
                        hasEntry("log-level", List.of("100", "200", "300"))
                )
        );
    }

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void positionalsAndFlagsAndKeywords(MagicsArgs schema, String args,
                                               Matcher<Map<String, List<String>>> matcher) {
        List<String> rawArgs = MagicsResolver.split(args);
        Map<String, List<String>> actual = Assertions.assertDoesNotThrow(() -> schema.parse(rawArgs));

        assertThat(actual, matcher);
    }

    public static Stream<Arguments> positionalsAndFlagsAndKeywords() {
        return Stream.of(
                Arguments.of(
                        args(b -> b.required("a")
                                .optional("b")
                                .flag("log-level", 'v', "100")
                                .keyword("log-level")),
                        "-v value-a --log-level=200 value-b --log-level 300",
                        allOf(
                                hasEntry("log-level", List.of("100", "200", "300")),
                                hasEntry("a", List.of("value-a")),
                                hasEntry("b", List.of("value-b"))
                        ))
        );
    }

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void strange(MagicsArgs schema, String args, Matcher<Map<String, List<String>>> matcher) {
        List<String> rawArgs = MagicsResolver.split(args);
        Map<String, List<String>> actual = Assertions.assertDoesNotThrow(() -> schema.parse(rawArgs));

        assertThat(actual, matcher);
    }

    public static Stream<Arguments> strange() {
        return Stream.of(
                Arguments.of(
                        args(b -> b.keyword("a")),
                        "\"--a=value with spaces\"",
                        hasEntry("a", List.of("value with spaces"))
                ),
                Arguments.of(
                        args(b -> b.keyword("a")),
                        "--a=\"value with spaces\"",
                        hasEntry("a", List.of("value with spaces"))
                ),
                Arguments.of(
                        args(b -> b.keyword("a")),
                        "--a \"value with spaces\"",
                        hasEntry("a", List.of("value with spaces"))
                )
        );
    }

    @ParameterizedTest(name = "{index}: \"{0}\" with \"{1}\"")
    @MethodSource
    public void any_throws(MagicsArgs schema, String args) {
        List<String> rawArgs = MagicsResolver.split(args);

        assertThrows(MagicArgsParseException.class, () -> schema.parse(rawArgs));
    }

    public static Stream<Arguments> any_throws() {
        return Stream.of(
                Arguments.of(
                        args(b -> b.required("a")),
                        ""
                ),
                Arguments.of(
                        args(b -> b.required("a")),
                        "value-a extra-a"
                ),
                Arguments.of(
                        args(b -> b.optional("a")),
                        "value-a extra-a"
                ),
                Arguments.of(
                        args(b -> b.onlyKnownKeywords()),
                        "--unknown=val"
                ),
                Arguments.of(
                        args(b -> b.onlyKnownKeywords()),
                        "--unknown val"
                ),
                Arguments.of(
                        args(b -> b.onlyKnownFlags()),
                        "-idk"
                ),
                Arguments.of(
                        args(b -> b.flag("test", 'i').onlyKnownFlags()),
                        "-idk"
                ),
                Arguments.of(
                        args(b -> b.keyword("a", MagicsArgs.KeywordSpec.ONCE)),
                        "--a a --a not-ok..."
                )
        );
    }

    private static MagicsArgs args(Consumer<MagicsArgs.MagicsArgsBuilder> config) {
        MagicsArgs.MagicsArgsBuilder builder = MagicsArgs.builder();
        config.accept(builder);
        return builder.build();
    }
}
