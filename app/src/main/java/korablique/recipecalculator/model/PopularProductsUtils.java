package korablique.recipecalculator.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PopularProductsUtils {
    private PopularProductsUtils() {}

    public static class FoodstuffFrequency implements Comparable<FoodstuffFrequency> {
        private Foodstuff foodstuff;
        private int frequency;

        public FoodstuffFrequency(Foodstuff foodstuff, int frequency) {
            this.foodstuff = foodstuff;
            this.frequency = frequency;
        }

        public Foodstuff getFoodstuff() {
            return foodstuff;
        }

        public int getFrequency() {
            return frequency;
        }

        @Override
        public int compareTo(@NonNull FoodstuffFrequency foodstuffFrequency) {
            return foodstuffFrequency.frequency - frequency;
        }
    }

    public static List<FoodstuffFrequency> getTop(List<Foodstuff> from) {
        // LinkedHashMap is used so that the insertion order would be preserved
        HashMap<Foodstuff, Integer> foodstuffsWithFrequency = new LinkedHashMap<>();
        for (Foodstuff foodstuff : from) {
            Integer currentCount = foodstuffsWithFrequency.get(foodstuff);
            if (currentCount == null) {
                currentCount = 0;
            }
            foodstuffsWithFrequency.put(foodstuff, currentCount + 1);
        }
        List<FoodstuffFrequency> result = new ArrayList<>();
        for (Map.Entry<Foodstuff, Integer> entry : foodstuffsWithFrequency.entrySet()) {
            result.add(new FoodstuffFrequency(entry.getKey(), entry.getValue()));
        }
        Collections.sort(result);
        return result;
    }
}
