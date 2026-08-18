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

class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    @BeforeEach
    void setUp() {
        summarizer = new NumberRangeSummarizerImpl();
    }

    @Test
    @DisplayName("The sample from the interface javadoc is summarized as specified")
    void summarizesTheSampleInput() {
        String input = "1,3,6,7,8,12,13,14,15,21,22,23,24,31";

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31",
                summarizer.summarizeCollection(summarizer.collect(input)));
    }

    @Nested
    @DisplayName("collect")
    class Collect {

        @Test
        void parsesEachNumberInOrder() {
            assertEquals(Arrays.asList(1, 3, 6), summarizer.collect("1,3,6"));
        }

        @Test
        void sortsUnorderedInput() {
            assertEquals(Arrays.asList(1, 3, 6), summarizer.collect("6,1,3"));
        }

        @Test
        void removesDuplicates() {
            assertEquals(Arrays.asList(1, 3), summarizer.collect("3,1,3,1"));
        }

        @Test
        void ignoresWhitespaceAroundNumbers() {
            assertEquals(Arrays.asList(1, 2, 3), summarizer.collect(" 1 , 2 ,\t3\n"));
        }

        @Test
        void skipsEmptyEntries() {
            assertEquals(Arrays.asList(1, 2), summarizer.collect("1,,2,"));
        }

        @Test
        void supportsNegativeNumbers() {
            assertEquals(Arrays.asList(-3, -2, -1), summarizer.collect("-1,-2,-3"));
        }

        @Test
        void supportsTheIntegerBounds() {
            assertEquals(Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE),
                    summarizer.collect(Integer.MAX_VALUE + "," + Integer.MIN_VALUE));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", ",", " , "})
        void returnsEmptyForBlankInput(String input) {
            assertTrue(summarizer.collect(input).isEmpty());
        }

        @Test
        void returnsEmptyForNullInput() {
            assertTrue(summarizer.collect(null).isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"1,two,3", "1,2.5", "1,2a", "1,2147483648"})
        void rejectsEntriesThatAreNotIntegers(String input) {
            assertThrows(IllegalArgumentException.class, () -> summarizer.collect(input));
        }

        @Test
        void returnsAnUnmodifiableCollection() {
            Collection<Integer> collected = summarizer.collect("1,2,3");

            assertThrows(UnsupportedOperationException.class, () -> collected.add(4));
        }
    }

    @Nested
    @DisplayName("summarizeCollection")
    class SummarizeCollection {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "'1', '1'",
                "'1,2', '1, 2'",
                "'1,2,3', '1-3'",
                "'1,3,5', '1, 3, 5'",
                "'1,2,3,7,8,9', '1-3, 7-9'",
                "'-3,-2,-1,0,1', '-3-1'",
                "'5,4,3,2,1', '1-5'"
        })
        void summarizesGroups(String input, String expected) {
            assertEquals(expected, summarizer.summarizeCollection(summarizer.collect(input)));
        }

        @Test
        @DisplayName("Two consecutive numbers are listed individually, not as a range")
        void doesNotRangeAPairOfNumbers() {
            assertEquals("1, 2, 5-7", summarizer.summarizeCollection(Arrays.asList(1, 2, 5, 6, 7)));
        }

        @Test
        void sortsAndDeduplicatesUnorderedInput() {
            Collection<Integer> unordered = new LinkedHashSet<>(Arrays.asList(8, 6, 7, 3, 1));

            assertEquals("1, 3, 6-8", summarizer.summarizeCollection(unordered));
        }

        @Test
        void returnsEmptyStringForAnEmptyCollection() {
            assertEquals("", summarizer.summarizeCollection(Collections.emptyList()));
        }

        @Test
        void returnsEmptyStringForNull() {
            assertEquals("", summarizer.summarizeCollection(null));
        }

        @Test
        void rejectsCollectionsContainingNull() {
            List<Integer> withNull = Arrays.asList(1, null, 3);

            assertThrows(IllegalArgumentException.class, () -> summarizer.summarizeCollection(withNull));
        }

        @Test
        @DisplayName("A range ending at Integer.MAX_VALUE does not overflow")
        void handlesTheUpperIntegerBound() {
            List<Integer> upperBound = Arrays.asList(
                    Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

            assertEquals((Integer.MAX_VALUE - 2) + "-" + Integer.MAX_VALUE,
                    summarizer.summarizeCollection(upperBound));
        }

        @Test
        void doesNotModifyTheInputCollection() {
            List<Integer> input = Arrays.asList(3, 1, 2);

            summarizer.summarizeCollection(input);

            assertEquals(Arrays.asList(3, 1, 2), input);
        }
    }
}
