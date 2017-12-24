package korablique.recipecalculator.model;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopularProductsUtils {
    private PopularProductsUtils() {}

    public static class FoodstuffFrequency implements Comparable<FoodstuffFrequency> {
        private long foodstuffId;
        private int frequency;

        public FoodstuffFrequency(long foodstuffId, int frequency) {
            this.foodstuffId = foodstuffId;
            this.frequency = frequency;
        }

        public long getFoodstuffId() {
            return foodstuffId;
        }

        public int getFrequency() {
            return frequency;
        }

        @Override
        public int compareTo(@NonNull FoodstuffFrequency foodstuffFrequency) {
            return frequency - foodstuffFrequency.frequency;
        }
    }

    public static List<FoodstuffFrequency> getTop(List<Long> from) {
        HashMap<Long, Integer> idsWithFrequency = new HashMap<>();
        for (Long id : from) {
            Integer currentCount = idsWithFrequency.get(id);
            if (currentCount == null) {
                currentCount = 0;
            }
            idsWithFrequency.put(id, currentCount + 1);
        }
        List<FoodstuffFrequency> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : idsWithFrequency.entrySet()) {
            result.add(new FoodstuffFrequency(entry.getKey(), entry.getValue()));
        }
        Collections.sort(result);
        Collections.reverse(result);
        return result;
    }
}
