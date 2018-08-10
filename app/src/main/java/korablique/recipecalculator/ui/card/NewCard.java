package korablique.recipecalculator.ui.card;


import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
    public static final int DEFAULT_WEIGHT = 100;
    private ViewGroup cardLayout;
    private Long foodstuffId;
    private EditText weightEditText;
    private TextView nameTextView;
    private Button addFoodstuffButton;

    private NutritionProgressWrapper nutritionProgressWrapper;
    private NutritionValuesWrapper nutritionValuesWrapper;

    public NewCard(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_card_layout, parent);
        addFoodstuffButton = cardLayout.findViewById(R.id.add_foodstuff_button);
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
        foodstuffId = foodstuff.getId();
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
                    foodstuffId,
                    nameTextView.getText().toString(),
                    Double.valueOf(weightEditText.getText().toString()),
                    nutritionValuesWrapper.getFoodstuff().getProtein(),
                    nutritionValuesWrapper.getFoodstuff().getFats(),
                    nutritionValuesWrapper.getFoodstuff().getCarbs(),
                    nutritionValuesWrapper.getFoodstuff().getCalories());
            listener.onClick(clickedFoodstuff);
        });
    }
}
