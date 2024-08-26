package org.dflib.jjava.jupyter.kernel.magic.registry;

import org.dflib.jjava.jupyter.kernel.magic.MagicParserTest;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class MagicsArgsTest {
    private static MagicsArgs args(Consumer<MagicsArgs.MagicsArgsBuilder> config) {
        MagicsArgs.MagicsArgsBuilder builder = MagicsArgs.builder();
        config.accept(builder);
        return builder.build();
    }

    private static List<String> list(String... args) {
        return Arrays.asList(args);
    }

    @Parameterized.Parameters(name = "{index}: \"{0}\" with \"{1}\"")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { args(b -> b.required("a")),
                        "value-a",
                        hasEntry("a", list("value-a")) },
                { args(b -> b.required("a").optional("b")),
                        "value-a",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list())
                        ) },
                { args(b -> b.required("a").optional("b")),
                        "value-a value-b",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list("value-b"))
                        ) },
                { args(b -> b.required("a").optional("b").varargs("c")),
                        "value-a value-b",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list("value-b")),
                                hasEntry("c", list())
                        ) },
                { args(b -> b.required("a").optional("b").varargs("c")),
                        "value-a value-b value-c",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list("value-b")),
                                hasEntry("c", list("value-c"))
                        ) },
                { args(b -> b.required("a").optional("b").varargs("c")),
                        "value-a value-b value-c-1 value-c-2",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list("value-b")),
                                hasEntry("c", list("value-c-1", "value-c-2"))
                        ) },
                { args(b -> b.required("a").required("b").varargs("c")),
                        "value-a value-b value-c-1 value-c-2",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list("value-b")),
                                hasEntry("c", list("value-c-1", "value-c-2"))
                        ) },
                { args(b -> b.optional("a")), "", hasEntry("a", list()) },
                { args(b -> b.optional("a").varargs("b")),
                        "",
                        allOf(
                                hasEntry("a", list()),
                                hasEntry("b", list())
                        ) },
                { args(b -> b.optional("a").varargs("b")),
                        "value-a",
                        allOf(
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list())
                        ) },
                { args(b -> b.optional("a").varargs("b")),
                        "value-a", allOf(
                        hasEntry("a", list("value-a")),
                        hasEntry("b", list())
                ) },
                { args(b -> b.varargs("a")),
                        "",
                        hasEntry("a", list()) },
                { args(b -> b.varargs("a")),
                        "value-a",
                        hasEntry("a", list("value-a")) },
                { args(b -> b.required("a").optional("a")),
                        "value-a extra-a",
                        hasEntry("a", list("value-a", "extra-a")) },
                { args(b -> b.required("a").optional("a")),
                        "value-a",
                        hasEntry("a", list("value-a")) },
                { args(b -> b.required("a").varargs("a")),
                        "value-a extra-a extra-a-2",
                        hasEntry("a", list("value-a", "extra-a", "extra-a-2")) },

                // FLAGS
                { args(b -> {}), "-f", hasEntry("f", list("")) },
                { args(b -> {}), "-fff", hasEntry("f", list("", "", "")) },
                { args(b -> {}), "-fg -g", allOf(
                        hasEntry("f", list("")),
                        hasEntry("g", list("", ""))
                ) },
                { args(b -> b.flag("test", 'f')), "", hasEntry("test", list()) },
                { args(b -> b.flag("verbose", 'v', "true")),
                        "-v",
                        hasEntry("verbose", list("true")) },

                // KEYWORDS
                { args(b -> {}), "--f=10", hasEntry("f", list("10")) },
                { args(b -> {}), "--f=10 --f=11", hasEntry("f", list("10", "11")) },
                { args(b -> {}), "--f 10 --f=11 --f 12", hasEntry("f", list("10", "11", "12")) },
                { args(b -> b.keyword("test")), "--test=10 --test 11 --test=12", hasEntry("test", list("10", "11", "12")) },
                { args(b -> b.keyword("test", MagicsArgs.KeywordSpec.REPLACE)),
                        "--test=10 --test 11 --test=12",
                        hasEntry("test", list("12")) },
                { args(b -> b.keyword("test")), "", hasEntry("test", list()) },

                // FLAGS and KEYWORDS
                { args(b -> b.flag("log-level", 'v', "100").keyword("log-level")),
                        "-v --log-level=200 --log-level 300",
                        hasEntry("log-level", list("100", "200", "300")) },

                // POSITIONALS and FLAGS and KEYWORDS
                { args(b -> b.required("a").optional("b").flag("log-level", 'v', "100").keyword("log-level")),
                        "-v value-a --log-level=200 value-b --log-level 300",
                        allOf(
                                hasEntry("log-level", list("100", "200", "300")),
                                hasEntry("a", list("value-a")),
                                hasEntry("b", list("value-b"))
                        ) },

                // Exceptions
                { args(b -> b.required("a")), "", null },
                { args(b -> b.required("a")), "value-a extra-a", null },
                { args(b -> b.optional("a")), "value-a extra-a", null },
                { args(b -> b.onlyKnownKeywords()), "--unknown=val", null },
                { args(b -> b.onlyKnownKeywords()), "--unknown val", null },
                { args(b -> b.onlyKnownFlags()), "-idk", null },
                { args(b -> b.flag("test", 'i').onlyKnownFlags()), "-idk", null },
                { args(b -> b.keyword("a", MagicsArgs.KeywordSpec.ONCE)), "--a a --a not-ok...", null },

                // Strange
                { args(b -> b.keyword("a")),
                        "\"--a=value with spaces\"",
                        hasEntry("a", list("value with spaces")) },
                { args(b -> b.keyword("a")),
                        "--a=\"value with spaces\"",
                        hasEntry("a", list("value with spaces")) },
                { args(b -> b.keyword("a")),
                        "--a \"value with spaces\"",
                        hasEntry("a", list("value with spaces")) },
        });
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private MagicsArgs schema;
    private String args;
    private Matcher<Map<String, List<String>>> test;

    public MagicsArgsTest(MagicsArgs schema, String args, Matcher<Map<String, List<String>>> test) {
        this.schema = schema;
        this.args = args;
        this.test = test;
    }

    @Test
    public void test() {
        List<String> rawArgs = MagicParserTest.split(this.args);
        if (this.test == null)
            exception.expect(MagicArgsParseException.class);

        Map<String, List<String>> args = this.schema.parse(rawArgs);

        if (this.test != null)
            assertThat(args, this.test);
    }
}