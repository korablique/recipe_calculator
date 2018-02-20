package korablique.recipecalculator.ui;


import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import korablique.recipecalculator.R;

public class NewCard {
    private ViewGroup cardLayout;

    public NewCard(Context context, ViewGroup parent) {
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_card_layout, parent);

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
}
