package org.dflib.jjava.jupyter.kernel.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FunctionalInterface
public interface CharPredicate {

    boolean test(char c);

    /**
     * Match any character in the {@code chars} string.
     *
     * @param chars a set of chars to match
     * @return a predicate that returns true when testing a character that is
     * the same as any character in the {@code chars} and false otherwise.
     */
    static CharPredicate anyOf(String chars) {
        int[] cs = chars.chars().sorted().distinct().toArray();
        return c -> {
            for (int cmpTo : cs) {
                if (cmpTo == c) return true;
                if (c < cmpTo) return false;
            }
            return false;
        };
    }

    class CharRange {
        public final char low;
        public final char high;

        public CharRange(char low, char high) {
            this.low = low;
            this.high = high;
        }
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private final List<CharRange> segments;

        public Builder() {
            this.segments = new ArrayList<>();
        }

        public Builder inRange(char low, char high) {
            if (high < low) {
                throw new IllegalArgumentException("Low char must be strictly less than high (low: " + low + ", high: " + high + ")");
            }

            segments.add(new CharRange(low, high));
            return this;
        }

        public Builder match(char c) {
            segments.add(new CharRange(c, c));
            return this;
        }

        public Builder match(String chars) {
            chars.chars().forEach(c -> segments.add(new CharRange((char) c, (char) c)));
            return this;
        }

        public CharPredicate build() {
            List<CharRange> ranges = new ArrayList<>(segments.size());

            if (!segments.isEmpty()) {
                segments.sort((range1, range2) ->
                        range1.low != range2.low
                                ? range1.low - range2.low
                                : range1.high - range2.high);

                Iterator<CharRange> itr = segments.iterator();
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

            CharRange[] test = ranges.toArray(new CharRange[0]);

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
