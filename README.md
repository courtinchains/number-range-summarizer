# Number Range Summarizer

An implementation of the `NumberRangeSummarizer` interface. It takes a comma delimited
list of numbers and returns a comma delimited summary in which runs of sequential
numbers are collapsed into a range.

```
Input:  "1,3,6,7,8,12,13,14,15,21,22,23,24,31"
Output: "1, 3, 6-8, 12-15, 21-24, 31"
```

## Requirements

- Java 8 or later
- Maven 3.6 or later

## Build and test

```bash
mvn clean test
```

## Usage

```java
NumberRangeSummarizer summarizer = new NumberRangeSummarizerImpl();
String summary = summarizer.summarizeCollection(summarizer.collect("1,3,6,7,8"));
// summary = "1, 3, 6-8"
```

## Design notes

- **`collect`** trims whitespace, skips empty entries (`"1,,2,"`), removes duplicates and
  returns the numbers in ascending order as an unmodifiable collection. A `null` or blank
  input returns an empty collection; an entry that is not a valid integer raises an
  `IllegalArgumentException` naming the offending entry rather than failing silently.
- **`summarizeCollection`** does not assume its input is sorted or unique, so it is
  correct whether or not the caller used `collect`. It never modifies the collection it
  is given. A `null` element raises an `IllegalArgumentException`.
- **Two consecutive numbers** are listed individually (`"1, 2"`) rather than as a range,
  because `1-2` is no shorter than the numbers it replaces.
- **Range boundaries** are compared with `long` arithmetic, so a run ending at
  `Integer.MAX_VALUE` cannot overflow.
- The implementation is stateless and therefore thread safe.

## Tests

32 JUnit 5 tests cover the sample input, parsing, ordering, duplicates, whitespace,
blank and `null` input, invalid entries, negative numbers, the `Integer` bounds,
single numbers, pairs, multiple ranges, immutability of the input, and the empty case.
