package korablique.recipecalculator.ui.addnewfoodstuff;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWrapper;

public class AddNewFoodstuffActivity extends BaseActivity {
    @Inject
    DatabaseWorker databaseWorker;
    private NutritionProgressWrapper nutritionProgressWrapper;
    private EditText foodstuffNameEditText;
    private EditText proteinEditText;
    private EditText fatsEditText;
    private EditText carbsEditText;
    private EditText caloriesEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_foodstuff);

        TextView titleTextView = findViewById(R.id.title_text);
        titleTextView.setText(R.string.new_foodstuff);

        saveButton = findViewById(R.id.save_button);
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

        foodstuffNameEditText = findViewById(R.id.foodstuff_name);
        proteinEditText = findViewById(R.id.protein_value);
        fatsEditText = findViewById(R.id.fats_value);
        carbsEditText = findViewById(R.id.carbs_value);
        caloriesEditText = findViewById(R.id.calories_value);

        nutritionProgressWrapper = new NutritionProgressWrapper(this, findViewById(R.id.nutrition_progress_bar));
        nutritionProgressWrapper.setNutrition(Nutrition.zero());
        updateSaveButtonEnability();
        TextWatcher nutritionChangeWatcher = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                double protein = parseNutrient(proteinEditText);
                double fats = parseNutrient(fatsEditText);
                double carbs = parseNutrient(carbsEditText);
                nutritionProgressWrapper.setNutrition(protein, fats, carbs);
            }
        };
        proteinEditText.addTextChangedListener(nutritionChangeWatcher);
        fatsEditText.addTextChangedListener(nutritionChangeWatcher);
        carbsEditText.addTextChangedListener(nutritionChangeWatcher);

        TextWatcher foodstuffInfoWatcher = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                updateSaveButtonEnability();
            }
        };
        foodstuffNameEditText.addTextChangedListener(foodstuffInfoWatcher);
        proteinEditText.addTextChangedListener(foodstuffInfoWatcher);
        fatsEditText.addTextChangedListener(foodstuffInfoWatcher);
        carbsEditText.addTextChangedListener(foodstuffInfoWatcher);
        caloriesEditText.addTextChangedListener(foodstuffInfoWatcher);

        View cancelButton = findViewById(R.id.button_close);
        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private Foodstuff parseFoodstuff() {
        String foodstuffName = foodstuffNameEditText.getText().toString();
        double protein = Double.parseDouble(proteinEditText.getText().toString());
        double fats = Double.parseDouble(fatsEditText.getText().toString());
        double carbs = Double.parseDouble(carbsEditText.getText().toString());
        double calories = Double.parseDouble(caloriesEditText.getText().toString());
        return new Foodstuff(foodstuffName, -1, protein, fats, carbs, calories);
    }

    private double parseNutrient(EditText editText) {
        String valueString = editText.getText().toString();
        if (valueString.isEmpty()) {
            return 0.0;
        } else {
            return Double.parseDouble(valueString);
        }
    }

    private void updateSaveButtonEnability() {
        if (foodstuffNameEditText.getText().toString().isEmpty()
                || proteinEditText.getText().toString().isEmpty()
                || fatsEditText.getText().toString().isEmpty()
                || carbsEditText.getText().toString().isEmpty()
                || caloriesEditText.getText().toString().isEmpty()) {
            saveButton.setEnabled(false);
        } else {
            saveButton.setEnabled(true);
        }
    }
}
