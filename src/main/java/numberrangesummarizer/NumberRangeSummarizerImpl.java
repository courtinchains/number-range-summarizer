package numberrangesummarizer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final String INPUT_DELIMITER = ",";
    private static final String OUTPUT_DELIMITER = ", ";
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
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> numbers = Stream.of(input.split(INPUT_DELIMITER))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(NumberRangeSummarizerImpl::parse)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return Collections.unmodifiableList(numbers);
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

        TreeSet<Integer> sorted = new TreeSet<>();
        for (Integer number : input) {
            if (number == null) {
                throw new IllegalArgumentException("Input contains a null element");
            }
            sorted.add(number);
        }

        StringBuilder summary = new StringBuilder();
        Integer rangeStart = null;
        Integer previous = null;

        for (Integer current : sorted) {
            if (rangeStart == null) {
                rangeStart = current;
            } else if (!isConsecutive(previous, current)) {
                appendGroup(summary, rangeStart, previous);
                rangeStart = current;
            }
            previous = current;
        }
        appendGroup(summary, rangeStart, previous);

        return summary.toString();
    }

    private static Integer parse(String entry) {
        try {
            return Integer.valueOf(entry);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid integer: \"" + entry + "\"", e);
        }
    }

    /** Uses long arithmetic so that a value of {@link Integer#MAX_VALUE} cannot overflow. */
    private static boolean isConsecutive(int previous, int current) {
        return (long) previous + 1L == (long) current;
    }

    private static void appendGroup(StringBuilder summary, int start, int end) {
        if (summary.length() > 0) {
            summary.append(OUTPUT_DELIMITER);
        }
        if (start == end) {
            summary.append(start);
        } else if (isConsecutive(start, end)) {
            summary.append(start).append(OUTPUT_DELIMITER).append(end);
        } else {
            summary.append(start).append(RANGE_DELIMITER).append(end);
        }
    }
}
