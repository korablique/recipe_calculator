package korablique.recipecalculator.database;

import org.joda.time.LocalDate;

import korablique.recipecalculator.database.room.FoodstuffEntity;
import korablique.recipecalculator.database.room.HistoryEntity;
import korablique.recipecalculator.database.room.UserParametersEntity;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.UserParameters;

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

    public static UserParameters toUserParameters(UserParametersEntity entity) {
        return new UserParameters(
                entity.getTargetWeight(),
                Gender.fromId(entity.getGenderId()),
                new LocalDate(entity.getYearOfBirth(), entity.getMonthOfBirth(), entity.getDayOfBirth()),
                entity.getHeight(),
                entity.getWeight(),
                Lifestyle.fromId(entity.getLifestyleId()),
                Formula.fromId(entity.getFormulaId()),
                entity.getMeasurementsTimestamp());
    }

    public static UserParametersEntity toEntity(UserParameters userParameters) {
        LocalDate dateOfBirth = userParameters.getDateOfBirth();
        int day = dateOfBirth.getDayOfMonth();
        int month = dateOfBirth.getMonthOfYear();
        int year = dateOfBirth.getYear();
        return new UserParametersEntity(
                userParameters.getTargetWeight(),
                userParameters.getGender().getId(),
                day, month, year,
                userParameters.getHeight(),
                userParameters.getWeight(),
                userParameters.getLifestyle().getId(),
                userParameters.getFormula().getId(),
                userParameters.getMeasurementsTimestamp());
    }
}
