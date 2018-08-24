package korablique.recipecalculator.model;

public class FoodstuffBuilderIDStep {
    final long id;

    FoodstuffBuilderIDStep(long id) {
        this.id = id;
    }

    public FoodstuffBuilderNameStep withName(String name) {
        return new FoodstuffBuilderNameStep(this, name);
    }
}
