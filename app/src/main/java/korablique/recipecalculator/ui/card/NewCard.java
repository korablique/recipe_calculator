package korablique.recipecalculator.ui.card;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWithValuesWrapper;

public class NewCard {
    public interface OnAddFoodstuffButtonClickListener {
        void onClick(Foodstuff foodstuff);
    }
    private ViewGroup cardLayout;
    private Long foodstuffId;
    private EditText weightEditText;
    private TextView nameTextView;

    private NutritionProgressWithValuesWrapper nutritionWrapper;

    public NewCard(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_card_layout, parent);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);
        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        nutritionWrapper = new NutritionProgressWithValuesWrapper(
                context, cardLayout.findViewById(R.id.nutrition_progress_with_values));
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        foodstuffId = foodstuff.getId();
        nameTextView.setText(foodstuff.getName());
        nutritionWrapper.setNutrition(Nutrition.of100gramsOf(foodstuff));
    }

    ViewGroup getCardLayout() {
        return cardLayout;
    }

    public void setOnAddFoodstuffButtonClickListener(OnAddFoodstuffButtonClickListener listener) {
        Button addFoodstuffButton = cardLayout.findViewById(R.id.add_foodstuff_button);
        addFoodstuffButton.setOnClickListener(v -> {
            Foodstuff clickedFoodstuff = new Foodstuff(
                    foodstuffId,
                    nameTextView.getText().toString(),
                    Double.valueOf(weightEditText.getText().toString()),
                    nutritionWrapper.getProteinValue(),
                    nutritionWrapper.getFatsValue(),
                    nutritionWrapper.getCarbsValue(),
                    nutritionWrapper.getCaloriesValue());
            listener.onClick(clickedFoodstuff);
        });
    }
}
