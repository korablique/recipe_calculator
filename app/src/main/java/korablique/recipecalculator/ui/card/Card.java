package korablique.recipecalculator.ui.card;


import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import androidx.annotation.StringRes;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.NutritionValuesWrapper;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class Card {
    public interface OnMainButtonClickListener {
        void onClick(WeightedFoodstuff foodstuff);
    }

    public interface OnEditButtonClickListener {
        void onClick(Foodstuff editingFoodstuff);
    }

    public interface OnCloseButtonClickListener {
        void onClick();
    }

    public interface OnDeleteButtonClickListener {
        void onClick(WeightedFoodstuff foodstuff);
    }

    public static final String EDITED_FOODSTUFF = "EDITED_FOODSTUFF";
    private ViewGroup cardLayout;
    private Foodstuff displayedFoodstuff;
    private EditText weightEditText;
    private TextView nameTextView;
    private Button button1;
    private Button button2;
    private View editButton;
    private View closeButton;
    private View deleteButton;

    private PluralProgressBar pluralProgressBar;
    private NutritionValuesWrapper nutritionValuesWrapper;

    public Card(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.card_layout, parent);
        button1 = cardLayout.findViewById(R.id.button1);
        button2 = cardLayout.findViewById(R.id.button2);
        editButton = cardLayout.findViewById(R.id.frame_layout_button_edit);
        closeButton = cardLayout.findViewById(R.id.button_close);
        deleteButton = cardLayout.findViewById(R.id.frame_layout_button_delete);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);
        updateMainButtonsEnability(weightEditText.getText());

        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        ViewGroup nutritionLayout = cardLayout.findViewById(R.id.nutrition_progress_with_values);
        pluralProgressBar = nutritionLayout.findViewById(R.id.new_nutrition_progress_bar);
        nutritionValuesWrapper = new NutritionValuesWrapper(context, nutritionLayout);
    }

    private void updateMainButtonsEnability(Editable text) {
        if (TextUtils.isEmpty(text)) {
            button1.setEnabled(false);
            button2.setEnabled(false);
        } else {
            button1.setEnabled(true);
            button2.setEnabled(true);
        }
    }

    public void setFoodstuff(WeightedFoodstuff weightedFoodstuff) {
        setFoodstuffImpl(weightedFoodstuff.withoutWeight());
        nutritionValuesWrapper.setNutrition(Nutrition.of(weightedFoodstuff));
        weightEditText.setText(toDecimalString(weightedFoodstuff.getWeight()));
        weightEditText.setSelection(weightEditText.getText().length());
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        setFoodstuffImpl(foodstuff);
        nutritionValuesWrapper.setNutrition(Nutrition.of100gramsOf(foodstuff));
    }

    private void setFoodstuffImpl(Foodstuff foodstuff) {
        displayedFoodstuff = foodstuff;
        nameTextView.setText(foodstuff.getName());
        Nutrition foodstuffNutrition = Nutrition.of100gramsOf(foodstuff);
        pluralProgressBar.setProgress(
                (float) foodstuffNutrition.getProtein(),
                (float) foodstuffNutrition.getFats(),
                (float) foodstuffNutrition.getCarbs());
        nutritionValuesWrapper.setFoodstuff(foodstuff);

        weightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                // пользователь отредактировал массу - показываем значения БЖУ на новую массу
                super.afterTextChanged(s);
                updateMainButtonsEnability(s);
                double newWeight = 0;
                if (!s.toString().isEmpty()) {
                    newWeight = Double.parseDouble(s.toString());
                }
                WeightedFoodstuff foodstuffWithNewWeight = foodstuff.withWeight(newWeight);
                Nutrition newNutrition = Nutrition.of(foodstuffWithNewWeight);
                nutritionValuesWrapper.setNutrition(newNutrition);
            }
        });
    }

    ViewGroup getCardLayout() {
        return cardLayout;
    }

    public void setUpButton1(OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(button1, listener, buttonTextRes);
    }

    public void setUpButton2(OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(button2, listener, buttonTextRes);
    }

    private void setUpMainButton(Button button, OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        button.setText(buttonTextRes);
        button.setOnClickListener(v -> {
            WeightedFoodstuff clickedFoodstuff = extractWeightedFoodstuff();
            listener.onClick(clickedFoodstuff);
        });
        button.setVisibility(View.VISIBLE);
    }

    private WeightedFoodstuff extractWeightedFoodstuff() {
        String weightText = weightEditText.getText().toString();
        if (TextUtils.isEmpty(weightText)) {
            weightText = "0";
        }
        return Foodstuff
                .withId(displayedFoodstuff.getId())
                .withName(nameTextView.getText().toString())
                .withNutrition(nutritionValuesWrapper.getFoodstuff().getProtein(),
                        nutritionValuesWrapper.getFoodstuff().getFats(),
                        nutritionValuesWrapper.getFoodstuff().getCarbs(),
                        nutritionValuesWrapper.getFoodstuff().getCalories())
                .withWeight(Double.valueOf(weightText));
    }

    void setOnEditButtonClickListener(OnEditButtonClickListener listener) {
        editButton.setOnClickListener(v -> {
            listener.onClick(displayedFoodstuff);
        });
    }

    void setOnCloseButtonClickListener(OnCloseButtonClickListener listener) {
        closeButton.setOnClickListener(v -> {
            listener.onClick();
        });
    }

    void setOnDeleteButtonClickListener(OnDeleteButtonClickListener listener) {
        deleteButton.setOnClickListener(v -> {
            WeightedFoodstuff clickedFoodstuff = extractWeightedFoodstuff();
            listener.onClick(clickedFoodstuff);
        });
    }

    void prohibitEditing(boolean flag) {
        if (flag) {
            editButton.setVisibility(View.GONE);
        } else {
            editButton.setVisibility(View.VISIBLE);
        }
    }

    void prohibitDeleting(boolean flag) {
        if (flag) {
            deleteButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }
    }
}
