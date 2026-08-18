package numberrangesummarizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Default {@link NumberRangeSummarizer} implementation.
 *
 * <p>The input is parsed into a sorted, duplicate-free collection of integers, which is
 * then rendered as a comma delimited string where runs of three or more consecutive
 * numbers are collapsed into a {@code start-end} range.</p>
 *
 * <p>This class is stateless and therefore thread safe.</p>
 */
public class NumberRangeSummarizerImpl implements NumberRangeSummarizer {

    /** Separates the numbers in the raw input string. */
    private static final String INPUT_DELIMITER = ",";

    /** Separates the groups in the summarized output, for example {@code "1, 3"}. */
    private static final String OUTPUT_DELIMITER = ", ";

    /** Joins the first and last number of a range, for example {@code "6-8"}. */
    private static final String RANGE_DELIMITER = "-";

    /**
     * Parses a comma delimited string of integers.
     *
     * <p>Surrounding whitespace around each number is ignored, empty entries (for example
     * the gap in {@code "1,,2"} or a trailing comma) are skipped, duplicates are removed
     * and the result is returned in ascending order.</p>
     *
     * @param input the comma delimited input; {@code null} or blank yields an empty collection
     * @return an unmodifiable, ascending, duplicate-free collection of the parsed numbers
     * @throws IllegalArgumentException if an entry is not a valid integer
     */
    @Override
    public Collection<Integer> collect(String input) {
        // Nothing to parse: treat missing input as "no numbers" rather than an error,
        // so that a caller can pipe collect() straight into summarizeCollection().
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // A TreeSet discards duplicates and maintains ascending order as entries are
        // inserted, so one pass does the work that a separate deduplicate-then-sort
        // would do in two. Ordering matters because ranges are only meaningful in
        // ascending order, and duplicates matter because a repeated number cannot
        // widen a range and so contributes nothing to the summary.
        TreeSet<Integer> numbers = new TreeSet<>();
        for (String entry : input.split(INPUT_DELIMITER)) {
            String trimmed = entry.trim();          // tolerate "1, 2 , 3"
            if (trimmed.isEmpty()) {
                continue;                           // tolerate "1,,2" and a trailing comma
            }
            numbers.add(parse(trimmed));            // fails loudly on a non-integer
        }

        // Copied into a List so the result is a compact, index-addressable snapshot, and
        // wrapped as unmodifiable because the parsed result is a value that callers
        // must not be able to corrupt.
        return Collections.unmodifiableList(new ArrayList<>(numbers));
    }

    /**
     * Summarizes a collection of integers, grouping sequential numbers into ranges.
     *
     * <p>The collection need not be sorted or free of duplicates: it is normalised before
     * summarizing. Two consecutive numbers are listed individually (for example
     * {@code "1, 2"}) because a range is no shorter than the numbers it replaces.</p>
     *
     * @param input the numbers to summarize; {@code null} or empty yields an empty string
     * @return the comma delimited summary, for example {@code "1, 3, 6-8, 12-15"}
     * @throws IllegalArgumentException if the collection contains a {@code null} element
     */
    @Override
    public String summarizeCollection(Collection<Integer> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Do not assume the caller used collect(): sort and deduplicate defensively, into a
        // new collection so that the caller's collection is never modified. A TreeSet does
        // both in one pass, and rejecting nulls here keeps the grouping loop below simple.
        TreeSet<Integer> sorted = new TreeSet<>();
        for (Integer number : input) {
            if (number == null) {
                throw new IllegalArgumentException("Input contains a null element");
            }
            sorted.add(number);
        }

        // Single pass over the sorted numbers. A group is an unbroken run of consecutive
        // values: rangeStart holds the first number of the run currently being built, and
        // previous holds the last number seen, which is the run's end once the run breaks.
        StringBuilder summary = new StringBuilder();
        Integer rangeStart = null;
        Integer previous = null;

        for (Integer current : sorted) {
            if (rangeStart == null) {
                // First number overall: open the first run.
                rangeStart = current;
            } else if (!isConsecutive(previous, current)) {
                // The run broke, so emit the completed run and open a new one at current.
                appendGroup(summary, rangeStart, previous);
                rangeStart = current;
            }
            // Otherwise current extends the open run and only previous needs updating.
            previous = current;
        }

        // The loop always leaves one run open, which is emitted here. This is safe because
        // the empty case returned early, so there is guaranteed to be at least one number.
        appendGroup(summary, rangeStart, previous);

        return summary.toString();
    }

    /**
     * Converts a single entry to an integer, translating the low-level parse failure into
     * an argument error that names the offending entry.
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
     * <p>The comparison is done in {@code long} arithmetic so that {@code previous + 1}
     * cannot overflow when {@code previous} is {@link Integer#MAX_VALUE}.</p>
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
        // Every group after the first is preceded by the output delimiter.
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
