package korablique.recipecalculator.ui.editfoodstuff;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.TwoOptionsDialog;
import korablique.recipecalculator.ui.calckeyboard.CalcEditText;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.numbersediting.EditProgressText;
import korablique.recipecalculator.ui.numbersediting.EditProgressTextCommonMaxController;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.ui.card.Card.EDITED_FOODSTUFF;

public class EditFoodstuffActivity extends BaseActivity {
    public static final String EDIT_FOODSTUFF_ACTION = "EDIT_FOODSTUFF_ACTION";
    public static final String EXTRA_RESULT_FOODSTUFF = "EXTRA_RESULT_FOODSTUFF";
    public static final String ARE_YOU_SURE_DIALOG_TAG = "ARE_YOU_SURE_DIALOG_TAG";
    @Inject
    FoodstuffsList foodstuffsList;
    @Inject
    CalcKeyboardController calcKeyboardController;
    private PluralProgressBar pluralProgressBar;
    private EditText foodstuffNameEditText;
    private EditProgressText proteinEditText;
    private EditProgressText fatsEditText;
    private EditProgressText carbsEditText;
    private CalcEditText caloriesEditText;
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

        calcKeyboardController.useCalcKeyboardWith(proteinEditText, this);
        calcKeyboardController.useCalcKeyboardWith(fatsEditText, this);
        calcKeyboardController.useCalcKeyboardWith(carbsEditText, this);
        calcKeyboardController.useCalcKeyboardWith(caloriesEditText, this);

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
                TwoOptionsDialog dialog = TwoOptionsDialog.showDialog(
                        getSupportFragmentManager(),
                        ARE_YOU_SURE_DIALOG_TAG,
                        getString(R.string.foodstuff_deletion_are_you_sure_dialog_title),
                        getString(R.string.foodstuff_deletion_are_you_sure_positive_response),
                        getString(R.string.foodstuff_deletion_are_you_sure_negative_response));
                initDeletionDialog(dialog, editingFoodstuff);
            });
            TwoOptionsDialog dialog = TwoOptionsDialog.findDialog(getSupportFragmentManager(), ARE_YOU_SURE_DIALOG_TAG);
            if (dialog != null) {
                initDeletionDialog(dialog, editingFoodstuff);
            }

            saveButton.setOnClickListener(v -> {
                Foodstuff editedFoodstuff = parseFoodstuff();
                long id = editingFoodstuff.getId();
                foodstuffsList.editFoodstuff(id, editedFoodstuff);

                Foodstuff editedFoodstuffWithId =
                        Foodstuff.withId(id)
                                .withName(editedFoodstuff.getName())
                                .withNutrition(Nutrition.of100gramsOf(editedFoodstuff));
                setResult(RESULT_OK, createResultIntent(editedFoodstuffWithId));
                finish();
            });
        } else {
            saveButton.setOnClickListener(v -> {
                Foodstuff foodstuff = parseFoodstuff();
                foodstuffsList.saveFoodstuff(
                        foodstuff,
                        new FoodstuffsList.SaveFoodstuffCallback() {
                    @Override
                    public void onResult(Foodstuff addedFoodstuff) {
                        Toast.makeText(EditFoodstuffActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK, createResultIntent(addedFoodstuff));
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

    private void initDeletionDialog(TwoOptionsDialog dialog, Foodstuff editingFoodstuff) {
        dialog.setOnButtonsClickListener(buttonName -> {
            if (buttonName == TwoOptionsDialog.ButtonName.POSITIVE) {
                foodstuffsList.deleteFoodstuff(editingFoodstuff);
                Intent intent = createResultIntent(null);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                dialog.dismiss();
            }
        });
    }

    public static void startForCreation(
            Fragment fragment,
            int requestCode) {
        Intent intent = new Intent(fragment.getContext(), EditFoodstuffActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static void startForEditing(
            Fragment fragment,
            Foodstuff foodstuff,
            int requestCode) {
        Intent intent = new Intent(fragment.getContext(), EditFoodstuffActivity.class);
        intent.setAction(EDIT_FOODSTUFF_ACTION);
        intent.putExtra(EDITED_FOODSTUFF, foodstuff);
        fragment.startActivityForResult(intent, requestCode);
    }

    private static Intent createResultIntent(@Nullable Foodstuff editedFoodstuff) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_FOODSTUFF, editedFoodstuff);
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
        double protein = parseNutrient(proteinEditText);
        double fats = parseNutrient(fatsEditText);
        double carbs = parseNutrient(carbsEditText);
        double calories = parseNutrient(caloriesEditText);
        return Foodstuff.withName(foodstuffName).withNutrition(protein, fats, carbs, calories);
    }

    private double parseNutrient(CalcEditText editText) {
        Float value = ((CalcEditText) editText).calcCurrentValue();
        if (value != null) {
            return value;
        } else {
            return 0.0;
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
