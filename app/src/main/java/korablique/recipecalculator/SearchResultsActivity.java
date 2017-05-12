package korablique.recipecalculator;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class SearchResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(ListOfFoodstuffsActivity.SEARCH_MESSAGE);
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(this);
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            Cursor cursor = database.query(
                    TABLE_NAME,
                    new String[]{
                            COLUMN_NAME_FOODSTUFF_NAME,
                            COLUMN_NAME_PROTEIN,
                            COLUMN_NAME_FATS,
                            COLUMN_NAME_CARBS,
                            COLUMN_NAME_CALORIES },
                    COLUMN_NAME_FOODSTUFF_NAME + " LIKE ?",
                    new String[]{ "%" + query + "%" },
                    null,
                    null,
                    COLUMN_NAME_FOODSTUFF_NAME + " ASC");
            LinearLayout parent = (LinearLayout) findViewById(R.id.search_results);
            if (cursor.moveToFirst()) {
                do {
                    Row row = new Row(this, parent);
                    row.getWeightTextView().setVisibility(View.GONE);
                    row.getNameTextView().setText(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME)));
                    row.getProteinTextView().setText(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_PROTEIN)));
                    row.getFatsTextView().setText(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FATS)));
                    row.getCarbsTextView().setText(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CARBS)));
                    row.getCaloriesTextView().setText(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_CALORIES)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
}
