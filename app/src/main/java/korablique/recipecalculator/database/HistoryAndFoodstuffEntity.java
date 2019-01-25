package korablique.recipecalculator.database;

import androidx.room.Embedded;

public class HistoryAndFoodstuffEntity {
    @Embedded
    HistoryEntity historyEntity;

    @Embedded
    FoodstuffEntity foodstuffEntity;
}
