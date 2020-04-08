package korablique.recipecalculator.database.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {FoodstuffEntity.class, UserParametersEntity.class, HistoryEntity.class,
        RecipeEntity.class, IngredientEntity.class},
        version = 7)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FoodstuffsDao foodstuffsDao();
    public abstract UserParametersDao userParametersDao();
    public abstract HistoryDao historyDao();
    public abstract RecipeDao recipeDao();
    public abstract IngredientDao ingredientDao();
}
