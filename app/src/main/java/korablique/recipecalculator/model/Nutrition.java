package korablique.recipecalculator.model;

public class Nutrition {
    private final double protein;
    private final double fats;
    private final double carbs;
    private final double calories;

    public static Nutrition of(WeightedFoodstuff foodstuff) {
        return new Nutrition(
                foodstuff.getProtein() * foodstuff.getWeight() / 100,
                foodstuff.getFats() * foodstuff.getWeight() / 100,
                foodstuff.getCarbs() * foodstuff.getWeight() / 100,
                foodstuff.getCalories() * foodstuff.getWeight() / 100);
    }

    public static Nutrition of100gramsOf(WeightedFoodstuff foodstuff) {
        return Nutrition.of(foodstuff.withoutWeight());
    }

    public static Nutrition of(Foodstuff foodstuff) {
        return new Nutrition(
                foodstuff.getProtein(),
                foodstuff.getFats(),
                foodstuff.getCarbs(),
                foodstuff.getCalories());
    }

    public static Nutrition zero() {
        return new Nutrition(0, 0, 0, 0);
    }

    public Nutrition plus(Nutrition nutrition) {
        return new Nutrition(
                this.protein + nutrition.protein,
                this.fats + nutrition.fats,
                this.carbs + nutrition.carbs,
                this.calories + nutrition.calories);
    }

    public Nutrition multiply(double factor) {
        return new Nutrition(
                this.protein * factor,
                this.fats * factor,
                this.carbs * factor,
                this.calories * factor);
    }

    private Nutrition(double protein, double fats, double carbs, double calories) {
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getFats() {
        return fats;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getCalories() {
        return calories;
    }
}
