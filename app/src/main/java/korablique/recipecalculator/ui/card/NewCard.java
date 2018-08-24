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

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWrapper;
import korablique.recipecalculator.ui.NutritionValuesWrapper;

public class NewCard {
    public interface OnAddFoodstuffButtonClickListener {
        void onClick(Foodstuff foodstuff);
    }

    public interface OnEditButtonClickListener {
        void onClick(Foodstuff editingFoodstuff);
    }

    public interface OnCloseButtonClickListener {
        void onClick();
    }

    public static final int DEFAULT_WEIGHT = 100;
    public static final String EDITED_FOODSTUFF = "EDITED_FOODSTUFF";
    private ViewGroup cardLayout;
    private Foodstuff displayedFoodstuff;
    private EditText weightEditText;
    private TextView nameTextView;
    private Button addFoodstuffButton;
    private View editButton;
    private View closeButton;

    private NutritionProgressWrapper nutritionProgressWrapper;
    private NutritionValuesWrapper nutritionValuesWrapper;

    public NewCard(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_card_layout, parent);
        addFoodstuffButton = cardLayout.findViewById(R.id.add_foodstuff_button);
        editButton = cardLayout.findViewById(R.id.frame_layout_button_edit);
        closeButton = cardLayout.findViewById(R.id.button_close);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);
        weightEditText.setText(String.valueOf(DEFAULT_WEIGHT));
        updateAddButtonEnability(weightEditText.getText());

        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        ViewGroup nutritionLayout = cardLayout.findViewById(R.id.nutrition_progress_with_values);
        nutritionProgressWrapper = new NutritionProgressWrapper(context, nutritionLayout);
        nutritionValuesWrapper = new NutritionValuesWrapper(context, nutritionLayout);
    }

    private void updateAddButtonEnability(Editable text) {
        if (TextUtils.isEmpty(text)) {
            addFoodstuffButton.setEnabled(false);
        } else {
            addFoodstuffButton.setEnabled(true);
        }
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        displayedFoodstuff = foodstuff;
        nameTextView.setText(foodstuff.getName());
        if (foodstuff.getWeight() != -1) {
            weightEditText.setText(String.valueOf(foodstuff.getWeight()));
            weightEditText.setSelection(weightEditText.getText().length());
        }
        nutritionProgressWrapper.setNutrition(Nutrition.of100gramsOf(foodstuff));
        nutritionValuesWrapper.setFoodstuff(foodstuff);
        if (foodstuff.getWeight() == -1) {
            // когда фудстафф только задали - показываем БЖУ на 100 г
            nutritionValuesWrapper.setNutrition(Nutrition.of100gramsOf(foodstuff));
        } else {
            // если у него уже есть масса - показываем БЖУ на эту массу
            nutritionValuesWrapper.setNutrition(Nutrition.of(foodstuff));
        }
        weightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                // пользователь отредактировал массу - показываем значения БЖУ на новую массу
                super.afterTextChanged(s);
                updateAddButtonEnability(s);
                double newWeight = 0;
                if (!s.toString().isEmpty()) {
                    newWeight = Double.parseDouble(s.toString());
                }
                Foodstuff foodstuffWithNewWeight = foodstuff.recreateWithWeight(newWeight);
                Nutrition newNutrition = Nutrition.of(foodstuffWithNewWeight);
                nutritionValuesWrapper.setNutrition(newNutrition);
            }
        });
    }

    ViewGroup getCardLayout() {
        return cardLayout;
    }

    public void setOnAddFoodstuffButtonClickListener(OnAddFoodstuffButtonClickListener listener) {
        addFoodstuffButton.setOnClickListener(v -> {
            Foodstuff clickedFoodstuff = new Foodstuff(
                    displayedFoodstuff.getId(),
                    nameTextView.getText().toString(),
                    Double.valueOf(weightEditText.getText().toString()),
                    nutritionValuesWrapper.getFoodstuff().getProtein(),
                    nutritionValuesWrapper.getFoodstuff().getFats(),
                    nutritionValuesWrapper.getFoodstuff().getCarbs(),
                    nutritionValuesWrapper.getFoodstuff().getCalories());
            listener.onClick(clickedFoodstuff);
        });
    }

    public void setOnEditButtonClickListener(OnEditButtonClickListener listener) {
        editButton.setOnClickListener(v -> {
            listener.onClick(displayedFoodstuff);
        });
    }

    public void setOnCloseButtonClickListener(OnCloseButtonClickListener listener) {
        closeButton.setOnClickListener(v -> {
            listener.onClick();
        });
    }

    public void prohibitEditing(boolean flag) {
        if (flag) {
            editButton.setVisibility(View.GONE);
        } else {
            editButton.setVisibility(View.VISIBLE);
        }
    }
}
