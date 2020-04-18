package korablique.recipecalculator.ui.card;


import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.NutritionValuesWrapper;
import korablique.recipecalculator.ui.calckeyboard.CalcEditText;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;
import korablique.recipecalculator.util.FloatUtils;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class Card {
    public interface OnMainButtonClickListener {
        void onClick(
                WeightedFoodstuff foodstuff,
                Foodstuff originalFoodstuff,
                @Nullable Double originalWeight);
    }

    public interface OnMainButtonSimpleClickListener {
        void onClick(WeightedFoodstuff foodstuff);
        default OnMainButtonClickListener convert() {
            return (WeightedFoodstuff foodstuff,
                    Foodstuff originalFoodstuff,
                    @Nullable Double originalWeight) -> onClick(foodstuff);
        }
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
    private final CalcKeyboardController calcKeyboardController;
    private ViewGroup cardLayout;
    private Foodstuff receivedFoodstuff;
    @Nullable
    private Double receivedWeight;
    private CalcEditText weightEditText;
    private TextView nameTextView;
    private Button button1;
    private Button button2;
    private View editButton;
    private View closeButton;
    private View deleteButton;

    private PluralProgressBar pluralProgressBar;
    private NutritionValuesWrapper nutritionValuesWrapper;

    public Card(BaseBottomDialog dialog, ViewGroup parent, CalcKeyboardController calcKeyboardController) {
        this.calcKeyboardController = calcKeyboardController;
        Context context = dialog.getContext();
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.card_layout, parent);
        button1 = cardLayout.findViewById(R.id.button1);
        button2 = cardLayout.findViewById(R.id.button2);
        editButton = cardLayout.findViewById(R.id.frame_layout_button_edit);
        closeButton = cardLayout.findViewById(R.id.button_close);
        deleteButton = cardLayout.findViewById(R.id.frame_layout_button_delete);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);
        updateMainButtonsEnability();

        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        ViewGroup nutritionLayout = cardLayout.findViewById(R.id.nutrition_progress_with_values);
        pluralProgressBar = nutritionLayout.findViewById(R.id.new_nutrition_progress_bar);
        nutritionValuesWrapper = new NutritionValuesWrapper(context, nutritionLayout);

        editButton.setVisibility(View.GONE);
        closeButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        calcKeyboardController.useCalcKeyboardWith(weightEditText, dialog);
    }

    private void updateMainButtonsEnability() {
        Float currentVal = weightEditText.getCurrentCalculatedValue();
        if (currentVal == null
                || FloatUtils.areFloatsEquals(0, currentVal)) {
            button1.setEnabled(false);
            button2.setEnabled(false);
        } else {
            button1.setEnabled(true);
            button2.setEnabled(true);
        }
    }

    public void setFoodstuff(WeightedFoodstuff weightedFoodstuff) {
        setFoodstuffImpl(weightedFoodstuff.withoutWeight(), weightedFoodstuff.getWeight());
        nutritionValuesWrapper.setNutrition(Nutrition.of(weightedFoodstuff));
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        setFoodstuffImpl(foodstuff, null);
        nutritionValuesWrapper.setNutrition(Nutrition.of100gramsOf(foodstuff));
    }

    private void setFoodstuffImpl(Foodstuff foodstuff, @Nullable Double weight) {
        receivedFoodstuff = foodstuff;
        receivedWeight = weight;
        nameTextView.setText(foodstuff.getName());
        Nutrition foodstuffNutrition = Nutrition.of100gramsOf(foodstuff);
        pluralProgressBar.setProgress(
                (float) foodstuffNutrition.getProtein(),
                (float) foodstuffNutrition.getFats(),
                (float) foodstuffNutrition.getCarbs());
        nutritionValuesWrapper.setFoodstuff(foodstuff);

        if (weight != null) {
            weightEditText.setText(toDecimalString(weight));
            weightEditText.setSelection(weightEditText.getText().length());
        }
        weightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                // пользователь отредактировал массу - показываем значения БЖУ на новую массу
                super.afterTextChanged(s);
                updateMainButtonsEnability();
                Float value = weightEditText.getCurrentCalculatedValue();
                double newWeight = 0;
                if (value != null) {
                    newWeight = value;
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

    public void setUpButton1(OnMainButtonSimpleClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(
                button1,
                (foodstuff, unused1, unused2) -> listener.onClick(foodstuff),
                buttonTextRes);
    }

    public void setUpButton2(OnMainButtonSimpleClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(
                button2,
                (foodstuff, unused1, unused2) -> listener.onClick(foodstuff),
                buttonTextRes);
    }

    private void setUpMainButton(Button button, OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        button.setText(buttonTextRes);
        button.setOnClickListener(v -> {
            WeightedFoodstuff clickedFoodstuff = extractWeightedFoodstuff();
            listener.onClick(clickedFoodstuff, receivedFoodstuff, receivedWeight);
        });
        button.setVisibility(View.VISIBLE);
    }

    public void deinitButton1() {
        deinitMainButton(button1);
    }

    public void deinitButton2() {
        deinitMainButton(button2);
    }

    private void deinitMainButton(Button button) {
        button.setOnClickListener(null);
        button.setVisibility(View.GONE);
    }

    private WeightedFoodstuff extractWeightedFoodstuff() {
        Float weight = weightEditText.getCurrentCalculatedValue();
        if (weight == null) {
            weight = 0f;
        }
        return Foodstuff
                .withId(receivedFoodstuff.getId())
                .withName(nameTextView.getText().toString())
                .withNutrition(nutritionValuesWrapper.getFoodstuff().getProtein(),
                        nutritionValuesWrapper.getFoodstuff().getFats(),
                        nutritionValuesWrapper.getFoodstuff().getCarbs(),
                        nutritionValuesWrapper.getFoodstuff().getCalories())
                .withWeight(weight);
    }

    public Foodstuff extractFoodstuff() {
        return extractWeightedFoodstuff().withoutWeight();
    }

    void setOnEditButtonClickListener(@Nullable OnEditButtonClickListener listener) {
        if (listener != null) {
            editButton.setVisibility(View.VISIBLE);
        } else {
            editButton.setVisibility(View.GONE);
        }
        editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(receivedFoodstuff);
            }
        });
    }

    void setOnCloseButtonClickListener(OnCloseButtonClickListener listener) {
        if (listener != null) {
            closeButton.setVisibility(View.VISIBLE);
        } else {
            closeButton.setVisibility(View.GONE);
        }
        closeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick();
            }
        });
    }

    void setOnDeleteButtonClickListener(OnDeleteButtonClickListener listener) {
        if (listener != null) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
        deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                WeightedFoodstuff clickedFoodstuff = extractWeightedFoodstuff();
                listener.onClick(clickedFoodstuff);
            }
        });
    }

    void focusOnEditing() {
        weightEditText.requestFocus();
    }
}
