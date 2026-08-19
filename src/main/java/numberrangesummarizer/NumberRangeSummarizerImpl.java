package numberrangesummarizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Default {@link NumberRangeSummarizer} implementation.
 */
public class NumberRangeSummarizerImpl implements NumberRangeSummarizer {

    /** Separates the numbers in the raw input string. */
    private static final String INPUT_DELIMITER = ",";

    /** Separates the groups in the summarized output, for example {@code "1, 3"}. */
    private static final String OUTPUT_DELIMITER = ", ";

    /** Joins the first and last number of a range, for example {@code "6-8"}. */
    private static final String RANGE_DELIMITER = "-";

    @Override
    public Collection<Integer> collect(String input) {
        // If no input data is given, the program should treat it as zero numbers instead of crashing
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // A TreeSet automatically removes the duplicate items and sorts from smallest to largest
        TreeSet<Integer> numbers = new TreeSet<>();
        for (String entry : input.split(INPUT_DELIMITER)) {
            String trimmed = entry.trim();          // tolerate "1, 2 , 3"
            if (trimmed.isEmpty()) {
                continue;                           // tolerate "1,,2" and a trailing comma
            }
            numbers.add(parse(trimmed));            // fails loudly on a non-integer
        }

        // Copied into a List so the result is fixed and locked; stopping other parts of program from breaking or changing data later
        return Collections.unmodifiableList(new ArrayList<>(numbers));
    }

    /**
     * Summarizes a collection of integers, grouping sequential numbers into ranges.
     */
    @Override
    public String summarizeCollection(Collection<Integer> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Sort and remove duplicates into a NEW collection, so the caller's collection is
        // never modified. This does not assume the caller used collect().
        // Any null value is rejected with an error, which keeps the grouping loop below simple.
        TreeSet<Integer> sorted = new TreeSet<>();
        for (Integer number : input) {
            if (number == null) {
                throw new IllegalArgumentException("Input contains a null element");
            }
            sorted.add(number);
        }

        // Go through the sorted numbers once and then group numbers together when they follow without any gaps
        // Note where each group starts and the last number added to it
        StringBuilder summary = new StringBuilder();
        Integer rangeStart = null;
        Integer previous = null;

        for (Integer current : sorted) {
            if (rangeStart == null) {
                // First number overall: open the first run.
                rangeStart = current;
            } else if (!isConsecutive(previous, current)) {
                // The sequence ended, so the completed range is added and a new one is started with the current number
                appendGroup(summary, rangeStart, previous);
                rangeStart = current;
            }
            // Else, the current number continues the existing sequence so just update the previous number
            previous = current;
        }

        // The loop always leaves one sequence unfinished, so we add it here
        // empty list is already handled meaning there is always at least 1 number
        appendGroup(summary, rangeStart, previous);

        return summary.toString();
    }

    /**
     * Converts one value into an integer, if the value is invalid, it gives an error that shows clearly
     * which value caused the issue
     */
    private static Integer parse(String entry) {
        try {
            return Integer.valueOf(entry);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid integer: \"" + entry + "\"", e);
        }
    }

    /**
     * Returns whether {@code current} immediately follows {@code previous}.
     *
     * <p>The comparison uses {@code long} arithmetic so that {@code previous + 1} cannot
     * overflow. Both current call sites pass {@code current > previous}, so {@code previous}
     * cannot itself be {@link Integer#MAX_VALUE} today: the wider arithmetic is defence
     * against a future caller, not a guard on a reachable path.</p>
     */
    private static boolean isConsecutive(int previous, int current) {
        return (long) previous + 1L == (long) current;
    }

    /**
     * Appends one completed group to the summary, choosing the shortest sensible form:
     * a single number, a pair listed individually, or a {@code start-end} range.
     *
     * @param summary the summary built so far
     * @param start   the first number of the group
     * @param end     the last number of the group, equal to {@code start} for a single number
     */
    private static void appendGroup(StringBuilder summary, int start, int end) {
        // Add a comma before every group except the first one.
        if (summary.length() > 0) {
            summary.append(OUTPUT_DELIMITER);
        }

        if (start == end) {
            // A run of one is just the number itself.
            summary.append(start);
        } else if (isConsecutive(start, end)) {
            // A run of two: "1-2" is no shorter than "1, 2", so list the numbers instead.
            summary.append(start).append(OUTPUT_DELIMITER).append(end);
        } else {
            // A run of three or more collapses into a range.
            summary.append(start).append(RANGE_DELIMITER).append(end);
        }
    }
}
