package numberrangesummarizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NumberRangeSummarizerImpl}.
 *
 * <p>The tests are grouped by the interface method under test, with the specified sample
 * input covered first as the end-to-end acceptance case. The tests are written against
 * the {@link NumberRangeSummarizer} interface type so that they exercise the published
 * contract rather than the implementation class.</p>
 */
class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    @BeforeEach
    void setUp() {
        // The summarizer is stateless, but a fresh instance per test keeps them independent.
        summarizer = new NumberRangeSummarizerImpl();
    }

    @Test
    @DisplayName("The sample from the interface javadoc is summarized as specified")
    void summarizesTheSampleInput() {
        // The worked example given in the exercise: the acceptance criterion for the whole task.
        String input = "1,3,6,7,8,12,13,14,15,21,22,23,24,31";

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31",
                summarizer.summarizeCollection(summarizer.collect(input)));
    }

    @Nested
    @DisplayName("collect")
    class Collect {

        @Test
        void parsesEachNumberInOrder() {
            // The simple case: well-formed input is parsed as given.
            assertEquals(Arrays.asList(1, 3, 6), summarizer.collect("1,3,6"));
        }

        @Test
        void sortsUnorderedInput() {
            // Ranges are only meaningful in ascending order, so collect() sorts.
            assertEquals(Arrays.asList(1, 3, 6), summarizer.collect("6,1,3"));
        }

        @Test
        void removesDuplicates() {
            // A number repeated in the input contributes nothing to the summary.
            assertEquals(Arrays.asList(1, 3), summarizer.collect("3,1,3,1"));
        }

        @Test
        void ignoresWhitespaceAroundNumbers() {
            // Robustness: hand-typed and pretty-printed input often carries stray whitespace.
            assertEquals(Arrays.asList(1, 2, 3), summarizer.collect(" 1 , 2 ,\t3\n"));
        }

        @Test
        void skipsEmptyEntries() {
            // Robustness: a doubled or trailing comma is tolerated rather than fatal.
            assertEquals(Arrays.asList(1, 2), summarizer.collect("1,,2,"));
        }

        @Test
        void supportsNegativeNumbers() {
            // The leading minus must be read as a sign, not as a delimiter.
            assertEquals(Arrays.asList(-3, -2, -1), summarizer.collect("-1,-2,-3"));
        }

        @Test
        void supportsTheIntegerBounds() {
            // Boundary case: both extremes of the int range parse without overflow.
            assertEquals(Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE),
                    summarizer.collect(Integer.MAX_VALUE + "," + Integer.MIN_VALUE));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", ",", " , "})
        void returnsEmptyForBlankInput(String input) {
            // Input that holds no numbers yields no numbers, rather than throwing.
            assertTrue(summarizer.collect(input).isEmpty());
        }

        @Test
        void returnsEmptyForNullInput() {
            // A missing input is treated the same as a blank one.
            assertTrue(summarizer.collect(null).isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"1,two,3", "1,2.5", "1,2a", "1,2147483648"})
        void rejectsEntriesThatAreNotIntegers(String input) {
            // Words, decimals, trailing characters and out-of-range values must fail loudly:
            // silently dropping an unparseable entry would produce a quietly wrong summary.
            assertThrows(IllegalArgumentException.class, () -> summarizer.collect(input));
        }

        @Test
        void returnsAnUnmodifiableCollection() {
            // The parsed result is a value, so a caller must not be able to mutate it.
            Collection<Integer> collected = summarizer.collect("1,2,3");

            assertThrows(UnsupportedOperationException.class, () -> collected.add(4));
        }
    }

    @Nested
    @DisplayName("summarizeCollection")
    class SummarizeCollection {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "'1', '1'",                     // a single number
                "'1,2', '1, 2'",                // a pair stays listed, see the test below
                "'1,2,3', '1-3'",               // three consecutive numbers become a range
                "'1,3,5', '1, 3, 5'",           // no two numbers are consecutive
                "'1,2,3,7,8,9', '1-3, 7-9'",    // two separate ranges
                "'-3,-2,-1,0,1', '-3-1'",       // a range spanning zero, negative start
                "'5,4,3,2,1', '1-5'"            // descending input is sorted first
        })
        void summarizesGroups(String input, String expected) {
            assertEquals(expected, summarizer.summarizeCollection(summarizer.collect(input)));
        }

        @Test
        @DisplayName("Two consecutive numbers are listed individually, not as a range")
        void doesNotRangeAPairOfNumbers() {
            // Deliberate design choice: "1-2" is no shorter than "1, 2", so a pair is listed.
            assertEquals("1, 2, 5-7", summarizer.summarizeCollection(Arrays.asList(1, 2, 5, 6, 7)));
        }

        @Test
        void sortsAndDeduplicatesUnorderedInput() {
            // summarizeCollection() is part of the public contract and may be called with a
            // collection that did not come from collect(), so it normalises defensively.
            Collection<Integer> unordered = new LinkedHashSet<>(Arrays.asList(8, 6, 7, 3, 1));

            assertEquals("1, 3, 6-8", summarizer.summarizeCollection(unordered));
        }

        @Test
        void returnsEmptyStringForAnEmptyCollection() {
            // Nothing to summarize produces an empty summary, not a stray delimiter.
            assertEquals("", summarizer.summarizeCollection(Collections.emptyList()));
        }

        @Test
        void returnsEmptyStringForNull() {
            // A missing collection is treated the same as an empty one.
            assertEquals("", summarizer.summarizeCollection(null));
        }

        @Test
        void rejectsCollectionsContainingNull() {
            // A null element is a caller error: it cannot be summarized or safely ignored.
            List<Integer> withNull = Arrays.asList(1, null, 3);

            assertThrows(IllegalArgumentException.class, () -> summarizer.summarizeCollection(withNull));
        }

        @Test
        @DisplayName("A range ending at Integer.MAX_VALUE does not overflow")
        void handlesTheUpperIntegerBound() {
            // Guards the long arithmetic in isConsecutive(): with int arithmetic,
            // MAX_VALUE + 1 wraps to MIN_VALUE and this range would be broken up.
            List<Integer> upperBound = Arrays.asList(
                    Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

            assertEquals((Integer.MAX_VALUE - 2) + "-" + Integer.MAX_VALUE,
                    summarizer.summarizeCollection(upperBound));
        }

        @Test
        void doesNotModifyTheInputCollection() {
            // Summarizing is a read-only operation: order and contents survive the call.
            List<Integer> input = Arrays.asList(3, 1, 2);

            summarizer.summarizeCollection(input);

            assertEquals(Arrays.asList(3, 1, 2), input);
        }
    }
}
