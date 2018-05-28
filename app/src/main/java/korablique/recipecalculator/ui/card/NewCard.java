package korablique.recipecalculator.ui.card;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import korablique.recipecalculator.FloatUtils;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWithNumbersWrapper;

public class NewCard {
    public interface OnAddFoodstuffButtonClickListener {
        void onClick(Foodstuff foodstuff);
    }
    private ViewGroup cardLayout;
    private EditText weightEditText;
    private TextView nameTextView;

    private NutritionProgressWithNumbersWrapper nutritionWrapper;

    public NewCard(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_card_layout, parent);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);
        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        nutritionWrapper = new NutritionProgressWithNumbersWrapper(
                context, cardLayout.findViewById(R.id.nutrition_progress_with_numbers));
    }

    public void setFoodstuff(Foodstuff foodstuff) {
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
