package korablique.recipecalculator.ui;


import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.mainscreen.CardDialog;

public class NewCard {
    private ViewGroup cardLayout;
    private Context context;
    private TextView nameTextView;
    private TextView proteinTextView;
    private TextView fatsTextView;
    private TextView carbsTextView;
    private TextView caloriesTextView;
    private EditText weightEditText;


    public NewCard(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_card_layout, parent);
        this.context = context;

        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        proteinTextView = cardLayout.findViewById(R.id.protein_layout).findViewById(R.id.nutrition_text_view);
        fatsTextView = cardLayout.findViewById(R.id.fats_layout).findViewById(R.id.nutrition_text_view);
        carbsTextView = cardLayout.findViewById(R.id.carbs_layout).findViewById(R.id.nutrition_text_view);
        caloriesTextView = cardLayout.findViewById(R.id.calories_layout).findViewById(R.id.nutrition_text_view);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);

        setNutritionTable(R.id.protein_layout, R.string.protein, R.drawable.new_card_protein_icon);
        setNutritionTable(R.id.fats_layout, R.string.fats, R.drawable.new_card_fats_icon);
        setNutritionTable(R.id.carbs_layout, R.string.carbs, R.drawable.new_card_carbs_icon);
        setNutritionTable(R.id.calories_layout, R.string.calories_per_100_g, R.drawable.invisible_drawable);

    }

    private void setNutritionTable(@IdRes int nutritionLayout, @StringRes int nutritionName, @DrawableRes int drawable) {
        ViewGroup layout = cardLayout.findViewById(nutritionLayout);
        TextView header = layout.findViewById(R.id.nutrition_name);
        header.setText(nutritionName);
        View coloredCircle = layout.findViewById(R.id.colored_circle);
        coloredCircle.setBackground(cardLayout.getResources().getDrawable(drawable));
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        nameTextView.setText(foodstuff.getName());
        setNutritionValue(proteinTextView, foodstuff.getProtein());
        setNutritionValue(fatsTextView, foodstuff.getFats());
        setNutritionValue(carbsTextView, foodstuff.getCarbs());
        setNutritionValue(caloriesTextView, foodstuff.getCalories());
    }

    private void setNutritionValue(TextView nutritionTextView, double nutritionValue) {
        nutritionTextView.setText(context.getString(R.string.one_digit_precision_float,
                nutritionValue).replace(',', '.'));
    }

    public ViewGroup getCardLayout() {
        return cardLayout;
    }

    public void setOnAddFoodstuffButtonClickListener(CardDialog.OnAddFoodstuffButtonClickListener listener) {
        Button addFoodstuffButton = cardLayout.findViewById(R.id.add_foodstuff_button);
        addFoodstuffButton.setOnClickListener(v -> {
            Foodstuff clickedFoodstuff = new Foodstuff(
                    nameTextView.getText().toString(),
                    Double.valueOf(weightEditText.getText().toString()),
                    Double.valueOf(proteinTextView.getText().toString()),
                    Double.valueOf(fatsTextView.getText().toString()),
                    Double.valueOf(carbsTextView.getText().toString()),
                    Double.valueOf(caloriesTextView.getText().toString()));
            listener.onClick(clickedFoodstuff);
        });
    }
}
