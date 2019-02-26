package korablique.recipecalculator.util;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import korablique.recipecalculator.base.Function1arg;
import me.xdrop.fuzzywuzzy.FuzzySearch;

/**
 * Utility class for performing fuzzy search (approximate string matching).
 */
public class FuzzySearcher {
    /**
     * A threshold we use to say which 2 items are similar and which aren't.
     * The {@link me.xdrop.fuzzywuzzy.FuzzySearch} class returns a similarity
     * value called 'ratio' when it compares strings. The bigger ratio is, the more
     * similar compared strings are.
     * Current value of SEARCH_RATIO_THRESHOLD is chosen arbitrary based on
     * manual testing of different threshold values. Feel free to change it if it doesn't satisfy
     * current needs.
     */
    private static final int SEARCH_RATIO_THRESHOLD = 70;
    private FuzzySearcher() {}

    /**
     * Performs fuzzy search, returns items which it considered to be similar to the search
     * query. Returned items are ordered from the most similar to the least similar.
     * @param query search query
     * @param items items among which search will be done
     * @param toStringFunction a functional object to turn items into strings
     * @param limit limit of items in result
     * @param <T> the type of items - can be any type
     * @return similar to the search query items, ordered from the most similar to the least
     */
    public static <T> List<T> search(String query,
                                     Collection<T> items,
                                     Function1arg<String, T> toStringFunction,
                                     int limit) {
        List<Pair<Integer, T>> resultWithRatio = new ArrayList<>();
        for (T item : items) {
            String itemString = toStringFunction.call(item);
            // The 'weightedRatio' function uses several fuzzy search
            // algorithms - it combines their output for better results.
            int ratio = FuzzySearch.weightedRatio(query, itemString);
            if (ratio >= SEARCH_RATIO_THRESHOLD) {
                resultWithRatio.add(Pair.create(ratio, item));
                if (resultWithRatio.size() >= limit) {
                    break;
                }
            }
        }

        // rhs.first-lhs.first - the bigger the ratio is,
        // the closer it should be to the start of the list.
        Comparator<Pair<Integer, T>> comparator = (lhs, rhs) -> rhs.first - lhs.first;
        Collections.sort(resultWithRatio, comparator);

        List<T> result = new ArrayList<>(resultWithRatio.size());
        for (Pair<Integer, T> itemWithRatio : resultWithRatio) {
            result.add(itemWithRatio.second);
        }
        return result;
    }
}
