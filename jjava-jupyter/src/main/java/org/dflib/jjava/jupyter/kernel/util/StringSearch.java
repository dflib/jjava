package org.dflib.jjava.jupyter.kernel.util;

public class StringSearch {
    public static class Range {
        private final int low;
        private final int high;

        public Range(int low, int high) {
            this.low = low;
            this.high = high;
        }

        public int getLow() {
            return low;
        }

        public int getHigh() {
            return high;
        }

        public int getLength() {
            return high - low;
        }

        public String extractSubString(String original) {
            return original.substring(low, high);
        }
    }

    /**
     * Find the longest substring such that all characters in the substring match the
     * {@code test}.
     *
     * @param code the code to preform the search in.
     * @param at   the position to start the search at. The returned range will contain this
     *             position. It is usually the position of a cursor.
     * @param test a predicate that must evaluate to true if a character should be included in
     *             the match.
     *
     * @return a range specifying the bounds of the longest match containing the {@code at}
     *         position. If nothing matches then this returns {@code null}.
     */
    public static Range findLongestMatchingAt(String code, int at, CharPredicate test) {
        if (test == null || at < 0 || at > code.length())
            return null;

        int start, end;
        if (at < code.length() && test.test(code.charAt(at))) {
            //The code[at] is a valid char and so worst case start = end is the entire string
            start = end = at;
        } else if (at > 0 && test.test(code.charAt(at - 1))) {
            //The code[at] isn't valid but the previous one is a good starting point
            //which may happen if "at" is immediately following a word
            start = end = at - 1;
        } else {
            return null;
        }

        while (start > 0 && test.test(code.charAt(start - 1)))
            start--;
        while (end < code.length() - 1 && test.test(code.charAt(end + 1)))
            end++;

        return new Range(start, end + 1);
    }
}
