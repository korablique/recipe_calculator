package korablique.recipecalculator;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class ListOfFoodstuffsActivity extends AppCompatActivity {
    private Card card;
    private MyAdapter recyclerViewAdapter;
    private View.OnClickListener onRowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            card.displayForRow(new Row(
                    ListOfFoodstuffsActivity.this,
                    (ViewGroup)findViewById(R.id.recycler_view),
                    (LinearLayout) v));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_foodstuffs);

        card = new Card(this, (ViewGroup)findViewById(R.id.list_of_recipes_parent));
        card.getButtonOk().setVisibility(View.GONE);
        card.getWeightTextView().setVisibility(View.GONE);
        card.getWeightEditText().setVisibility(View.GONE);
        card.hide();
        card.getButtonSave().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (card.getNameEditText().getText().toString().isEmpty()
                        || card.getProteinEditText().getText().toString().isEmpty()
                        || card.getFatsEditText().getText().toString().isEmpty()
                        || card.getCarbsEditText().getText().toString().isEmpty()
                        || card.getCaloriesEditText().getText().toString().isEmpty()) {
                    Toast.makeText(ListOfFoodstuffsActivity.this, "Заполните название и БЖУК", Toast.LENGTH_LONG).show();
                    return;
                }
                String newName = card.getNameEditText().getText().toString();
                double newProtein = Double.parseDouble(card.getProteinEditText().getText().toString());
                double newFats = Double.parseDouble(card.getFatsEditText().getText().toString());
                double newCarbs = Double.parseDouble(card.getCarbsEditText().getText().toString());
                double newCalories = Double.parseDouble(card.getCaloriesEditText().getText().toString());
                //сохраняем новые значения в базу данных
                int id = (int)card.getEditedRow().getRowLayout().getTag();
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(ListOfFoodstuffsActivity.this);
                SQLiteDatabase database = dbHelper.getWritableDatabase();

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newName);
                contentValues.put(COLUMN_NAME_PROTEIN, newProtein);
                contentValues.put(COLUMN_NAME_FATS, newFats);
                contentValues.put(COLUMN_NAME_CARBS, newCarbs);
                contentValues.put(COLUMN_NAME_CALORIES, newCalories);
                database.update(TABLE_NAME, contentValues, "id = ?", new String[]{ String.valueOf(id) });
                recyclerViewAdapter.notifyDataSetChanged();
                Toast.makeText(ListOfFoodstuffsActivity.this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                card.hide();
                KeyboardHandler keyboardHandler = new KeyboardHandler(ListOfFoodstuffsActivity.this);
                keyboardHandler.hideKeyBoard();

            }
        });
        card.getButtonDelete().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = (int)card.getEditedRow().getRowLayout().getTag();
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(ListOfFoodstuffsActivity.this);
                SQLiteDatabase database = dbHelper.getWritableDatabase();
                database.delete(TABLE_NAME, "id = ?", new String[]{ String.valueOf(id) });
                recyclerViewAdapter.notifyDataSetChanged();
                recyclerViewAdapter.deleteItem(id);
                Toast.makeText(ListOfFoodstuffsActivity.this, "Продукт удалён", Toast.LENGTH_SHORT).show();
                card.hide();
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        recyclerViewAdapter = new MyAdapter(db, onRowClickListener);
        recyclerView.setAdapter(recyclerViewAdapter);
    }
}
