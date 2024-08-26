package org.dflib.jjava.jupyter.kernel.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@FunctionalInterface
public interface CharPredicate {

    public boolean test(char c);

    public default CharPredicate and(CharPredicate condition) {
        return c -> this.test(c) && condition.test(c);
    }

    public default CharPredicate or(CharPredicate condition) {
        return c -> this.test(c) || condition.test(c);
    }

    public default CharPredicate not() {
        return new NotCharPredicate(this);
    }

    public static class NotCharPredicate implements CharPredicate {
        private final CharPredicate test;

        public NotCharPredicate(CharPredicate test) {
            this.test = test;
        }

        @Override
        public boolean test(char c) {
            return !this.test.test(c);
        }

        @Override
        public CharPredicate not() {
            return this.test;
        }
    }

    /**
     * Match characters that fall between the given character bounds (inclusive).
     *
     * @param low  the lower bound of the range (inclusive)
     * @param high the upper bound of the range (inclusive)
     *
     * @return a predicate that returns true when testing a character in this range
     *         and false otherwise.
     */
    public static CharPredicate inRange(char low, char high) {
        return c -> low <= c && c <= high;
    }

    /**
     * Match a character that is the same as the {@code match} character.
     *
     * @param match the character to match with
     *
     * @return a predicate that returns true when testing a character that is
     *         the same as the {@code match} character and false otherwise.
     */
    public static CharPredicate match(char match) {
        return c -> c == match;
    }

    /**
     * Match any character in the {@code chars} string.
     *
     * @param chars a set of chars to match
     *
     * @return a predicate that returns true when testing a character that is
     *         the same as any character in the {@code chars} and false otherwise.
     */
    public static CharPredicate anyOf(String chars) {
        int[] cs = chars.chars().sorted().distinct().toArray();
        return c -> {
            for (int cmpTo : cs) {
                if (cmpTo == c) return true;
                if (c < cmpTo) return false;
            }
            return false;
        };
    }

    public static class CharRange {
        public final char low;
        public final char high;

        public CharRange(char low, char high) {
            this.low = low;
            this.high = high;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<CharRange> segments;

        public Builder() {
            this.segments = new LinkedList<>();
        }

        public Builder inRange(char low, char high) {
            if (high < low)
                throw new IllegalArgumentException("Low char must be strictly less than high (low: " + low + ", high: " + high + ")");

            this.segments.add(new CharRange(low, high));
            return this;
        }

        public Builder match(char c) {
            this.segments.add(new CharRange(c, c));
            return this;
        }

        public Builder match(String chars) {
            chars.chars().forEach(c -> this.segments.add(new CharRange((char) c, (char) c)));
            return this;
        }

        public CharPredicate build() {
            List<CharRange> ranges = new ArrayList<>(this.segments.size());

            if (!this.segments.isEmpty()) {
                this.segments.sort((range1, range2) ->
                        range1.low != range2.low
                                ? range1.low - range2.low
                                : range1.high - range2.high);

                Iterator<CharRange> itr = this.segments.iterator();
                CharRange prev = itr.next();
                while (itr.hasNext()) {
                    CharRange next = itr.next();
                    if (prev.high < next.low) {
                        ranges.add(prev);
                        prev = next;
                    } else {
                        prev = new CharRange(prev.low, (char) Math.max(prev.high, next.high));
                    }
                }
                ranges.add(prev);
            }

            CharRange[] test = ranges.toArray(new CharRange[ranges.size()]);

            return c -> {
                for (CharRange range : test) {
                    if (c < range.low) return false;
                    if (c <= range.high) return true;
                }
                return false;
            };
        }
    }
}
