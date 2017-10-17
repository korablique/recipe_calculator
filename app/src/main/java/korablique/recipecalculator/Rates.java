package korablique.recipecalculator;

public class Rates {
    private float calories;
    private float protein;
    private float fats;
    private float carbs;

    public Rates(float calories, float protein, float fats, float carbs) {
        this.calories = calories;
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
    }

    public float getCalories() {
        return calories;
    }

    public float getProtein() {
        return protein;
    }

    public float getFats() {
        return fats;
    }

    public float getCarbs() {
        return carbs;
    }
}
