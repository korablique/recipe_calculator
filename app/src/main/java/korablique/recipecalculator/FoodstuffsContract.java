package korablique.recipecalculator;

import android.provider.BaseColumns;

public final class FoodstuffsContract {
    private FoodstuffsContract() {}

    public static abstract class Foodstuffs implements BaseColumns {
        public static final String ID = "ID";
        public static final String TABLE_NAME = "foodstuffs";
        public static final String COLUMN_NAME_FOODSTUFF_NAME = "foodstuff_name";
        public static final String COLUMN_NAME_PROTEIN = "protein";
        public static final String COLUMN_NAME_FATS = "fats";
        public static final String COLUMN_NAME_CARBS = "carbs";
        public static final String COLUMN_NAME_CALORIES = "calories";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String FLOAT_TYPE = " REAL"; //или назвать строку REAL?
    private static final String COMMA_SEP = ", ";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Foodstuffs.TABLE_NAME + " (" +
                    Foodstuffs.ID + " INTEGER PRIMARY KEY, " +
                    Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME + TEXT_TYPE + COMMA_SEP +
                    Foodstuffs.COLUMN_NAME_PROTEIN + FLOAT_TYPE + COMMA_SEP +
                    Foodstuffs.COLUMN_NAME_FATS + FLOAT_TYPE + COMMA_SEP +
                    Foodstuffs.COLUMN_NAME_CARBS + FLOAT_TYPE + COMMA_SEP +
                    Foodstuffs.COLUMN_NAME_CALORIES + FLOAT_TYPE + " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Foodstuffs.TABLE_NAME;
}

