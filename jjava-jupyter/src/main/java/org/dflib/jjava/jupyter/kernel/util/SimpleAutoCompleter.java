package org.dflib.jjava.jupyter.kernel.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A utility class to implement a prefix based auto completion algorithm. It
 * is a good basic implementation for completing keywords or identifiers that
 * have already been parsed or are in the current cell.
 */
public class SimpleAutoCompleter {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Collection<String> keywords;
        private boolean caseSensitive = true;
        private Comparator<String> resultsSorter = null;

        private Builder() {
            this.keywords = new ArrayList<>();
        }

        public Builder withKeywords(String... keywords) {
            Collections.addAll(this.keywords, keywords);
            return this;
        }

        public Builder withKeywords(Collection<String> keywords) {
            this.keywords.addAll(keywords);
            return this;
        }

        public Builder caseSensitive() {
            this.caseSensitive = true;
            return this;
        }

        public Builder caseInsensitive() {
            this.caseSensitive = false;
            return this;
        }

        private void addSorter(Comparator<String> comparator) {
            this.resultsSorter = this.resultsSorter == null ? comparator : this.resultsSorter.thenComparing(comparator);
        }

        public Builder preferShort() {
            addSorter(SHORTER_BETTER);
            return this;
        }

        public Builder preferLong() {
            addSorter(LONGER_BETTER);
            return this;
        }

        public Builder preferSmallerChars() {
            addSorter(this.caseSensitive ? LOWER_ALPHA_BETTER_CASE : LOWER_ALPHA_BETTER_NO_CASE);
            return this;
        }

        public Builder preferLargerChars() {
            addSorter(this.caseSensitive ? HIGHER_ALPHA_BETTER_CASE : HIGHER_ALPHA_BETTER_NO_CASE);
            return this;
        }

        public SimpleAutoCompleter build() {
            return new SimpleAutoCompleter(
                    this.keywords,
                    this.caseSensitive,
                    this.resultsSorter
            );
        }
    }

    private static final Comparator<String> SHORTER_BETTER = Comparator.comparingInt(String::length);
    private static final Comparator<String> LONGER_BETTER = SHORTER_BETTER.reversed();

    private static final Comparator<String> LOWER_ALPHA_BETTER_CASE = String::compareTo;
    private static final Comparator<String> HIGHER_ALPHA_BETTER_CASE = LOWER_ALPHA_BETTER_CASE.reversed();

    private static final Comparator<String> LOWER_ALPHA_BETTER_NO_CASE = String::compareToIgnoreCase;
    private static final Comparator<String> HIGHER_ALPHA_BETTER_NO_CASE = LOWER_ALPHA_BETTER_NO_CASE.reversed();

    protected final SortedSet<String> keywords;
    protected final Comparator<String> resultsSorter;

    public SimpleAutoCompleter(Collection<String> keywords, boolean caseSensitive, Comparator<String> resultsSorter) {
        this.keywords = new TreeSet<>(caseSensitive ? String::compareTo : String::compareToIgnoreCase);
        this.keywords.addAll(keywords);
        this.resultsSorter = resultsSorter;
    }

    public List<String> autocomplete(String prefix) {
        SortedSet<String> results = keywords.subSet(prefix, prefix + Character.MAX_VALUE);
        List<String> sortedResults = new ArrayList<>(results.size());
        sortedResults.addAll(results);
        if (this.resultsSorter != null && sortedResults.size() > 1)
            sortedResults.sort(this.resultsSorter);
        return sortedResults;
    }
}
