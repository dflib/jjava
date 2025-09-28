package org.dflib.jjava.jupyter.kernel.history;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public interface HistoryManager {
    enum ResultFlag {
        /**
         * Signals that the results should include the transformed output rather than
         * the raw output.
         */
        TRANSFORMED_INPUT,

        /**
         * Signals that the results should include the cell output in addition to the
         * input. When set, the manager should take care to include an empty string
         * when there is no output rather than {@code null}.
         */
        INCLUDE_OUTPUT,

        /**
         * Signals that all results should include unique inputs only.
         */
        UNIQUE,
    }

    /**
     * Lookup a specified range of input cells executed by the kernel that this manager
     * is working for.
     *
     * @param sessionOffset an offset index describing the session to search. The current session is represented by 0,
     *                      the previous by -1, and so on.
     * @param startCell     the index (inclusive) of the first cell to include in the results.
     * @param endCell       the index (exclusive) of the last cell to include in the results.
     * @param flags         result affecting flags. Inclusion in the set specifies that the flag is set.
     * @return a list of history entries in the range.
     */
    default List<HistoryEntry> lookupRange(int sessionOffset, int startCell, int endCell, Set<ResultFlag> flags) {
        return null;
    }

    /**
     * Lookup a specified range of input cells executed by the kernel that this manager
     * is working for.
     *
     * @param sessionOffset an offset index describing the session to search. The current session is represented by 0,
     *                      the previous by -1, and so on.
     * @param startCell     the index (inclusive) of the first cell to include in the results.
     * @param endCell       the index (exclusive) of the last cell to include in the results.
     * @param flags         result affecting flags. Inclusion in the set specifies that the flag is set.
     * @return a list of history entries in the range or {@code null} if the method is not supported.
     */
    default List<HistoryEntry> lookupRange(int sessionOffset, int startCell, int endCell, ResultFlag... flags) {
        Set<ResultFlag> flagSet = EnumSet.noneOf(ResultFlag.class);
        Collections.addAll(flagSet, flags);
        return lookupRange(sessionOffset, startCell, endCell, flagSet);
    }

    /**
     * Lookup the last {@code length} input cells executed by the kernel that this manager
     * is working for.
     *
     * @param length the number of results to include in the results.
     * @param flags  result affecting flags. Inclusion in the set specifies that the flag is set.
     * @return a list of the last {@code length} entries in the history or {@code null} if the method is not supported.
     */
    default List<HistoryEntry> lookupTail(int length, Set<ResultFlag> flags) {
        return null;
    }

    /**
     * Lookup the last {@code length} input cells executed by the kernel that this manager
     * is working for.
     *
     * @param length the number of results to include in the results.
     * @param flags  result affecting flags. Inclusion in the set specifies that the flag is set.
     * @return a list of the last {@code length} entries in the history or {@code null} if the method is not supported.
     */
    default List<HistoryEntry> lookupTail(int length, ResultFlag... flags) {
        Set<ResultFlag> flagSet = EnumSet.noneOf(ResultFlag.class);
        Collections.addAll(flagSet, flags);
        return lookupTail(length, flagSet);
    }

    /**
     * Lookup the last {@code length} input cells that match the {@code pattern}.
     * <p>
     * The {@code pattern} is an sqlite glob. More specifically:
     * <ul>
     * <li>asterisk ({@code *}) matches 0 or more of any characters</li>
     * <li>question mark ({@code ?}) matches exactly 1 of any character</li>
     * <li>
     * list wildcard ({@code []}) matches any character from the list
     * <ul>
     * <li>character ranges are supported with {@code [a-z]} syntax to match {@code a} to {@code z} inclusive</li>
     * <li>starting a list wildcard with {@code ^} negates the wildcard</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param pattern a glob pattern that input cells must match.
     * @param length  the number of results to include in the results.
     * @param flags   result affecting flags. Inclusion in the set specifies that the flag is set.
     * @return a list of the last {@code length} entries in the history that match the {@code pattern} or {@code null}
     * if the method is not supported.
     */
    default List<HistoryEntry> search(String pattern, int length, Set<ResultFlag> flags) {
        return null;
    }

    /**
     * Lookup the last {@code length} input cells that match the {@code pattern}.
     * <p>
     * The {@code pattern} is an sqlite glob. More specifically:
     * <ul>
     *   <li>asterisk ({@code *}) matches 0 or more of any characters</li>
     *   <li>question mark ({@code ?}) matches exactly 1 of any character</li>
     *   <li>
     *       list wildcard ({@code []}) matches any character from the list
     *       <ul>
     *         <li>character ranges are supported with {@code [a-z]} syntax to match {@code a} to {@code z} inclusive</li>
     *         <li>starting a list wildcard with {@code ^} negates the wildcard</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param pattern a glob pattern that input cells must match.
     * @param length  the number of results to include in the results.
     * @param flags   result affecting flags. Inclusion in the set specifies that the flag is set.
     * @return a list of the last {@code length} entries in the history that match the {@code pattern} or {@code null}
     * if the method is not supported.
     */
    default List<HistoryEntry> search(String pattern, int length, ResultFlag... flags) {
        Set<ResultFlag> flagSet = EnumSet.noneOf(ResultFlag.class);
        Collections.addAll(flagSet, flags);
        return search(pattern, length, flagSet);
    }
}
