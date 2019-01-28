package korablique.recipecalculator.database;

import korablique.recipecalculator.database.room.FoodstuffEntity;
import korablique.recipecalculator.database.room.HistoryEntity;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;

public class EntityConverter {
    private EntityConverter() {}

    public static FoodstuffEntity toEntity(Foodstuff foodstuff, int isListed) {
        return new FoodstuffEntity(
                foodstuff.getName(),
                foodstuff.getName().toLowerCase(),
                (float) foodstuff.getProtein(),
                (float) foodstuff.getFats(),
                (float) foodstuff.getCarbs(),
                (float) foodstuff.getCalories(),
                isListed);
    }

    public static Foodstuff toModel(FoodstuffEntity entity) {
        return Foodstuff
                .withId(entity.getId())
                .withName(entity.getName())
                .withNutrition(entity.getProtein(), entity.getFats(), entity.getCarbs(), entity.getCalories());
    }

    public static HistoryEntity toEntity(NewHistoryEntry historyEntry) {
        return new HistoryEntity(
                historyEntry.getDate().getTime(),
                historyEntry.getFoodstuffId(),
                (float) historyEntry.getFoodstuffWeight());
    }

    public static HistoryEntity toEntity(HistoryEntry historyEntry) {
        return new HistoryEntity(
                historyEntry.getTime().getTime(),
                historyEntry.getHistoryId(),
                (float) historyEntry.getFoodstuff().getWeight());
    }
}
