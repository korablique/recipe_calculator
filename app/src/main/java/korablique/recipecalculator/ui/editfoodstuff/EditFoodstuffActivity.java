package korablique.recipecalculator.ui.editfoodstuff;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.numbersediting.EditProgressText;
import korablique.recipecalculator.ui.numbersediting.EditProgressTextCommonMaxController;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;

import static korablique.recipecalculator.IntentConstants.EDIT_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.EDIT_RESULT;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.ui.card.Card.EDITED_FOODSTUFF;

public class EditFoodstuffActivity extends BaseActivity {
    public static final String EDIT_FOODSTUFF_ACTION = "korablique.recipecalculator.EDIT_FOODSTUFF_ACTION";
    @Inject
    FoodstuffsList foodstuffsList;
    private PluralProgressBar pluralProgressBar;
    private EditText foodstuffNameEditText;
    private EditProgressText proteinEditText;
    private EditProgressText fatsEditText;
    private EditProgressText carbsEditText;
    private EditText caloriesEditText;
    private Button saveButton;
    private EditProgressTextCommonMaxController nutritionsCommonMaxController;

    @Override
    protected Integer getLayoutId() {
        return R.layout.activity_add_new_foodstuff;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView titleTextView = findViewById(R.id.title_text);
        titleTextView.setText(R.string.new_foodstuff);

        foodstuffNameEditText = findViewById(R.id.foodstuff_name);
        proteinEditText = findViewById(R.id.protein_value);
        fatsEditText = findViewById(R.id.fats_value);
        carbsEditText = findViewById(R.id.carbs_value);
        caloriesEditText = findViewById(R.id.calories_value);
        saveButton = findViewById(R.id.save_button);

        pluralProgressBar = findViewById(R.id.new_nutrition_progress_bar);
        pluralProgressBar.setProgress(0, 0, 0);
        updateSaveButtonEnability();
        TextWatcher nutritionChangeWatcher = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                double protein = parseNutrient(proteinEditText);
                double fats = parseNutrient(fatsEditText);
                double carbs = parseNutrient(carbsEditText);
                // Sum of nutrition bigger that 100 is not allowed
                if (protein > 100) {
                    protein = 100;
                }
                if (protein + fats > 100) {
                    fats = 100 - protein;
                }
                if (protein + fats + carbs > 100) {
                    carbs = 100 - protein - fats;
                }
                pluralProgressBar.setProgress((float) protein, (float) fats, (float) carbs);
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

        nutritionsCommonMaxController =
                new EditProgressTextCommonMaxController(
                        100f, proteinEditText, fatsEditText, carbsEditText);
        nutritionsCommonMaxController.init();

        Intent receivedIntent = getIntent();
        if (EDIT_FOODSTUFF_ACTION.equals(receivedIntent.getAction())) {
            titleTextView.setText(R.string.change_foodstuff);
            Foodstuff editingFoodstuff = receivedIntent.getParcelableExtra(EDITED_FOODSTUFF);
            setDisplayingFoodstuff(editingFoodstuff);

            View deleteButton = findViewById(R.id.button_delete);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> {
                foodstuffsList.deleteFoodstuff(editingFoodstuff);
                Intent intent = createEditingResultIntent(null);
                setResult(RESULT_OK, intent);
                finish();
            });

            saveButton.setOnClickListener(v -> {
                Foodstuff editedFoodstuff = parseFoodstuff();
                long id = editingFoodstuff.getId();
                foodstuffsList.editFoodstuff(id, editedFoodstuff);

                Foodstuff editedFoodstuffWithId =
                        Foodstuff.withId(id)
                                .withName(editedFoodstuff.getName())
                                .withNutrition(Nutrition.of100gramsOf(editedFoodstuff));
                Intent intent = createEditingResultIntent(editedFoodstuffWithId);
                setResult(RESULT_OK, intent);
                finish();
            });
        } else {
            saveButton.setOnClickListener(v -> {
                foodstuffsList.saveFoodstuff(
                        parseFoodstuff(),
                        new FoodstuffsList.SaveFoodstuffCallback() {
                    @Override
                    public void onResult(long id) {
                        Toast.makeText(EditFoodstuffActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onDuplication() {
                        Toast.makeText(EditFoodstuffActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    public static void startForCreation(Context context) {
        Intent intent = new Intent(context, EditFoodstuffActivity.class);
        context.startActivity(intent);
    }

    public static void startForEditing(Fragment fragment, Foodstuff foodstuff) {
        Intent intent = new Intent(fragment.getContext(), EditFoodstuffActivity.class);
        intent.setAction(EDIT_FOODSTUFF_ACTION);
        intent.putExtra(EDITED_FOODSTUFF, foodstuff);
        fragment.startActivityForResult(intent, EDIT_FOODSTUFF_REQUEST);
    }

    public static Intent createEditingResultIntent(@Nullable Foodstuff editedFoodstuff) {
        Intent intent = new Intent();
        intent.putExtra(EDIT_RESULT, editedFoodstuff);
        return intent;
    }

    private void setDisplayingFoodstuff(Foodstuff editingFoodstuff) {
        foodstuffNameEditText.setText(editingFoodstuff.getName());
        proteinEditText.setText(toDecimalString(editingFoodstuff.getProtein()));
        fatsEditText.setText(toDecimalString(editingFoodstuff.getFats()));
        carbsEditText.setText(toDecimalString(editingFoodstuff.getCarbs()));
        caloriesEditText.setText(toDecimalString(editingFoodstuff.getCalories()));

        Nutrition editingFoodstuffNutrition = Nutrition.of100gramsOf(editingFoodstuff);
        pluralProgressBar.setProgress(
                (float) editingFoodstuffNutrition.getProtein(),
                (float) editingFoodstuffNutrition.getFats(),
                (float) editingFoodstuffNutrition.getCarbs());
    }

    private Foodstuff parseFoodstuff() {
        String foodstuffName = foodstuffNameEditText.getText().toString();
        double protein = Double.parseDouble(proteinEditText.getText().toString());
        double fats = Double.parseDouble(fatsEditText.getText().toString());
        double carbs = Double.parseDouble(carbsEditText.getText().toString());
        double calories = Double.parseDouble(caloriesEditText.getText().toString());
        return Foodstuff.withName(foodstuffName).withNutrition(protein, fats, carbs, calories);
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
