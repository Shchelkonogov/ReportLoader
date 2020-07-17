package ru.tecon.parser.types.pdf;

import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class TrapRangeBuilder {

    private final List<Range<Integer>> ranges = new ArrayList<>();

    void addRange(Range<Integer> range) {
        ranges.add(range);
    }

	List<Range<Integer>> build() {
        List<Range<Integer>> retVal = new ArrayList<>();
        ranges.sort(Comparator.comparing(Range::lowerEndpoint));
        for (Range<Integer> range : ranges) {
            if (retVal.isEmpty()) {
                retVal.add(range);
            } else {
                Range<Integer> lastRange = retVal.get(retVal.size() - 1);
                if (lastRange.isConnected(range)) {
                    Range<Integer> newLastRange = lastRange.span(range);
                    retVal.set(retVal.size() - 1, newLastRange);
                } else {
                    retVal.add(range);
                }
            }
        }
        return retVal;
    }
}