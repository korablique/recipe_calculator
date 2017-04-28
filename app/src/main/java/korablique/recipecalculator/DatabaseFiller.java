package korablique.recipecalculator;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;

public class DatabaseFiller {
    private DatabaseFiller() {}

    public static void fillDbOnFirstAppStart(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_FOODSTUFF_NAME, "помидор");
        values.put(COLUMN_NAME_PROTEIN, 0.6);
        values.put(COLUMN_NAME_FATS, 0.2);
        values.put(COLUMN_NAME_CARBS, 4.2);
        values.put(COLUMN_NAME_CALORIES, 20);

        db.insert(FoodstuffsContract.Foodstuffs.TABLE_NAME, null, values);
    }
}
