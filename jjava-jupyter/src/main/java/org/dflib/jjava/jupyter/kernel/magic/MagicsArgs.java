package org.dflib.jjava.jupyter.kernel.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MagicsArgs {
    public enum KeywordSpec {
        ONCE,
        COLLECT,
        REPLACE
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> requiredPositional = new ArrayList<>();
        private final List<String> optionalPositional = new ArrayList<>();
        private String varargs;

        private boolean acceptAnyKeyword = true;
        private boolean acceptAnyFlag = true;

        private final Map<String, Set<KeywordSpec>> keywords = new LinkedHashMap<>();
        private final Map<Character, String> flags = new LinkedHashMap<>();
        private final Map<String, String> flagDefaultValues = new LinkedHashMap<>();

        public Builder required(String name) {
            if (!this.optionalPositional.isEmpty() || this.varargs != null) {
                throw new IllegalStateException("Schema cannot have required positional arguments after optional ones.");
            }

            this.requiredPositional.add(name);
            return this;
        }

        public Builder optional(String name) {
            this.optionalPositional.add(name);
            return this;
        }

        public Builder varargs(String name) {
            this.varargs = name;
            return this;
        }

        // --keyword value or --keyword=value
        public Builder keyword(String name, KeywordSpec spec, KeywordSpec... specRest) {
            this.keywords.put(name, EnumSet.of(spec, specRest));
            return this;
        }

        public Builder keyword(String name) {
            return this.keyword(name, KeywordSpec.COLLECT);
        }

        public Builder flag(String name, char shortName, String value) {
            this.keyword(name);
            this.flags.put(shortName, name);
            this.flagDefaultValues.put(name, value);

            return this;
        }

        public Builder flag(String name, char shortName) {
            this.keyword(name);
            this.flags.put(shortName, name);

            return this;
        }

        public Builder anyKeyword() {
            this.acceptAnyKeyword = true;

            return this;
        }

        public Builder onlyKnownKeywords() {
            this.acceptAnyKeyword = false;

            return this;
        }

        public Builder anyFlag() {
            this.acceptAnyFlag = true;

            return this;
        }

        public Builder onlyKnownFlags() {
            this.acceptAnyFlag = false;

            return this;
        }

        private KeywordAggregator buildKeyword(Set<KeywordSpec> spec) {
            if (spec.contains(KeywordSpec.ONCE)) {
                return (name, value, rest, args) -> {
                    if (args.containsKey(name) && !args.get(name).isEmpty())
                        throw new MagicArgsParseException("'%s' may only be specified once.", name);

                    if (value != null) {
                        args.put(name, Collections.singletonList(value));

                        return rest;
                    } else {
                        if (rest.isEmpty())
                            throw new MagicArgsParseException("'%s' is a keyword argument but no value was supplied.", name);

                        args.put(name, Collections.singletonList(rest.get(0)));

                        return rest.subList(1, rest.size());
                    }
                };
            } else if (spec.contains(KeywordSpec.REPLACE)) {
                return (name, value, rest, args) -> {
                    if (value != null) {
                        args.put(name, Collections.singletonList(value));

                        return rest;
                    } else {
                        if (rest.isEmpty())
                            throw new MagicArgsParseException("'%s' is a keyword argument but no value was supplied.", name);

                        args.put(name, Collections.singletonList(rest.get(0)));

                        return rest.subList(1, rest.size());
                    }
                };
            } else /*default: if (spec.contains(KeywordSpec.COLLECT))*/ {
                return (name, value, rest, args) -> {
                    args.compute(name, (k, values) -> {
                        if (values == null)
                            values = new ArrayList<>();

                        if (value != null) {
                            values.add(value);
                        } else {
                            if (rest.isEmpty())
                                throw new MagicArgsParseException("'%s' is a keyword argument but no value was supplied.", name);

                            values.add(rest.get(0));
                        }

                        return values;
                    });

                    return value != null ? rest : rest.subList(1, rest.size());
                };
            }
        }

        public MagicsArgs build() {
            Map<String, KeywordAggregator> kw = new HashMap<>((int) Math.ceil(keywords.size() / 0.75));
            keywords.forEach((name, spec) -> kw.put(name, buildKeyword(spec)));

            return new MagicsArgs(
                    new ArrayList<>(this.requiredPositional),
                    new ArrayList<>(this.optionalPositional),
                    this.varargs,
                    kw,
                    this.flags,
                    this.flagDefaultValues,
                    this.acceptAnyKeyword ? this.buildKeyword(EnumSet.noneOf(KeywordSpec.class)) : null,
                    this.acceptAnyFlag ? this.buildKeyword(EnumSet.noneOf(KeywordSpec.class)) : null
            );
        }
    }

    @FunctionalInterface
    private interface KeywordAggregator {
        /**
         * Consume the argument.
         *
         * @param name  the name of the argument
         * @param value the value attached to the keyword or null
         * @param rest  the remaining arguments
         * @param args  the collection to append to
         * @return the new {@code rest}
         */
        List<String> consume(String name, String value, List<String> rest, Map<String, List<String>> args) throws MagicArgsParseException;
    }

    private static final Pattern KEYWORD_ARG_PATTERN = Pattern.compile("^--(?<name>[^=]+)(?:=(?<val>.+))?$");
    private static final Pattern FLAG_ARG_PATTERN = Pattern.compile("^-(?<flags>[a-zA-Z]+)$");

    private final List<String> positional;
    private final List<String> optional;
    private final String varargs;

    private final Map<String, KeywordAggregator> keywords;
    private final Map<Character, String> keywordFromFlag;
    private final Map<String, String> flagSuppliedDefaults;

    private final KeywordAggregator defaultKeywordAggregator;
    private final KeywordAggregator defaultFlagAggregator;

    MagicsArgs(
            List<String> positional,
            List<String> optional,
            String varargs,
            Map<String, KeywordAggregator> keywords,
            Map<Character, String> keywordFromFlag,
            Map<String, String> flagSuppliedDefaults,
            KeywordAggregator defaultKeywordAggregator,
            KeywordAggregator defaultFlagAggregator) {

        this.positional = positional;
        this.optional = optional;
        this.varargs = varargs;
        this.keywords = keywords;
        this.keywordFromFlag = keywordFromFlag;
        this.flagSuppliedDefaults = flagSuppliedDefaults;
        this.defaultKeywordAggregator = defaultKeywordAggregator;
        this.defaultFlagAggregator = defaultFlagAggregator;
    }

    public Map<String, List<String>> parse(List<String> args) throws MagicArgsParseException {

        Map<String, List<String>> collectedArgs = new LinkedHashMap<>();
        positional.forEach(a -> collectedArgs.put(a, new ArrayList<>()));
        optional.forEach(a -> collectedArgs.put(a, new ArrayList<>()));
        keywords.keySet().forEach(a -> collectedArgs.put(a, new ArrayList<>()));
        if (varargs != null) {
            collectedArgs.put(varargs, new ArrayList<>());
        }

        int positionalsMatched = 0;

        while (!args.isEmpty()) {
            String arg = args.get(0);
            args = args.subList(1, args.size());

            Matcher m = KEYWORD_ARG_PATTERN.matcher(arg);
            if (m.matches()) {
                String name = m.group("name");
                String value = m.group("val");

                KeywordAggregator aggregator = this.keywords.getOrDefault(name, this.defaultKeywordAggregator);

                if (aggregator == null) {
                    throw new MagicArgsParseException("Unknown keyword argument '%s'.", name);
                }

                args = aggregator.consume(name, value, args, collectedArgs);

                continue;
            }

            m = FLAG_ARG_PATTERN.matcher(arg);
            if (m.matches()) {
                String flags = m.group("flags");
                for (int i = 0; i < flags.length(); i++) {
                    char c = flags.charAt(i);

                    String name = this.keywordFromFlag.getOrDefault(c, Character.toString(c));

                    KeywordAggregator aggregator = this.keywords.getOrDefault(name, this.defaultFlagAggregator);

                    if (aggregator == null) {
                        throw new MagicArgsParseException("Unknown flag argument '%s'.", name);
                    }

                    args = aggregator.consume(name, this.flagSuppliedDefaults.getOrDefault(name, ""), args, collectedArgs);
                }

                continue;
            }

            if (positionalsMatched < this.positional.size())
                collectedArgs.compute(this.positional.get(positionalsMatched), (n, values) -> {
                    values = values != null ? values : new ArrayList<>();
                    values.add(arg);
                    return values;
                });
            else if (positionalsMatched < this.positional.size() + this.optional.size())
                collectedArgs.compute(this.optional.get(positionalsMatched - this.positional.size()), (n, values) -> {
                    values = values != null ? values : new ArrayList<>();
                    values.add(arg);
                    return values;
                });
            else if (varargs != null)
                collectedArgs.get(varargs).add(arg);
            else {
                throw new MagicArgsParseException("Too many positional arguments.");
            }

            positionalsMatched += 1;
        }

        if (positionalsMatched < this.positional.size()) {
            throw new MagicArgsParseException(
                    "Missing required positional arguments: %s",
                    positional.subList(positionalsMatched, positional.size()));
        }

        return collectedArgs;
    }

    @Override
    public String toString() {
        StringJoiner s = new StringJoiner(" ");

        this.positional.forEach(s::add);
        this.optional.forEach(a -> s.add("[" + a + "]"));
        if (this.varargs != null) {
            s.add(this.varargs + "...");
        }

        this.keywordFromFlag.keySet().forEach(c -> s.add("-" + c));
        if (this.defaultFlagAggregator != null) {
            s.add("-*");
        }

        this.keywords.keySet().stream()
                .filter(a -> !this.keywordFromFlag.values().contains(a))
                .forEach(a -> s.add("--" + a));

        if (this.defaultKeywordAggregator != null) {
            s.add("--**");
        }

        return s.toString();
    }
}
