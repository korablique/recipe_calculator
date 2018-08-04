package korablique.recipecalculator.ui.addnewfoodstuff;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;

public class AddNewFoodstuffActivity extends BaseActivity {
    @Inject
    DatabaseWorker databaseWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_foodstuff);

        TextView titleTextView = findViewById(R.id.title_text);
        titleTextView.setText(R.string.new_foodstuff);

        View cancelButton = findViewById(R.id.button_close);
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            databaseWorker.saveFoodstuff(AddNewFoodstuffActivity.this, parseFoodstuff(), new DatabaseWorker.SaveFoodstuffCallback() {
                @Override
                public void onResult(long id) {
                    Toast.makeText(AddNewFoodstuffActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onDuplication() {
                    Toast.makeText(AddNewFoodstuffActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private Foodstuff parseFoodstuff() {
        EditText foodstuffNameEdittext = findViewById(R.id.foodstuff_name);
        String foodstuffName = foodstuffNameEdittext.getText().toString();
        EditText proteinEdittext = findViewById(R.id.protein_value);
        double protein = Double.parseDouble(proteinEdittext.getText().toString());
        EditText fatsEdittext = findViewById(R.id.fats_value);
        double fats = Double.parseDouble(fatsEdittext.getText().toString());
        EditText carbsEdittext = findViewById(R.id.carbs_value);
        double carbs = Double.parseDouble(carbsEdittext.getText().toString());
        EditText caloriesEdittext = findViewById(R.id.calories_value);
        double calories = Double.parseDouble(caloriesEdittext.getText().toString());
        return new Foodstuff(foodstuffName, -1, protein, fats, carbs, calories);
    }
}
