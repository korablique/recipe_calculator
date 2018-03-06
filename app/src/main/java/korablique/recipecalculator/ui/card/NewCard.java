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

public class NewCard {
    public interface OnAddFoodstuffButtonClickListener {
        void onClick(Foodstuff foodstuff);
    }
    private static final int PROGRESSBAR_CORNERS_RADIUS = 4;
    private ViewGroup cardLayout;
    private Context context;
    private TextView nameTextView;
    private TextView proteinTextView;
    private TextView fatsTextView;
    private TextView carbsTextView;
    private TextView caloriesTextView;
    private EditText weightEditText;

    private static class NutritionWithProgress {
        TextView nutritionTextView;
        View progressView;

        public NutritionWithProgress(TextView nutritionTextView, View progressView) {
            this.nutritionTextView = nutritionTextView;
            this.progressView = progressView;
        }
    }

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

    // только задает цвета кружкам и названия в шапке
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

        // set progress in progressbar
        View proteinProgress = cardLayout.findViewById(R.id.protein_progress);
        setNutritionProgress(proteinProgress, foodstuff.getProtein());
        View fatsProgress = cardLayout.findViewById(R.id.fats_progress);
        setNutritionProgress(fatsProgress, foodstuff.getFats());
        View carbsProgress = cardLayout.findViewById(R.id.carbs_progress);
        setNutritionProgress(carbsProgress, foodstuff.getCarbs());
        View nothing = cardLayout.findViewById(R.id.nothing_progress);
        setNutritionProgress(nothing, 100 - foodstuff.getProtein() - foodstuff.getFats() - foodstuff.getCarbs());

        NutritionWithProgress[] nutritionsWithProgress = new NutritionWithProgress[]{
                new NutritionWithProgress(proteinTextView, proteinProgress),
                new NutritionWithProgress(fatsTextView, fatsProgress),
                new NutritionWithProgress(carbsTextView, carbsProgress)};
        roundCorners(nutritionsWithProgress);
    }

    private void roundCorners(NutritionWithProgress[] nutritionsWithProgress) {
        // find the most left line
        NutritionWithProgress left = null;
        for (int index = 0; index < nutritionsWithProgress.length; index++) {
            double progress = Double.valueOf(nutritionsWithProgress[index].nutritionTextView.getText().toString());
            if (!FloatUtils.areFloatsEquals(progress, 0)) {
                left = nutritionsWithProgress[index];
                break;
            }
        }
        // find the most right line
        NutritionWithProgress right = null;
        for (int index = nutritionsWithProgress.length - 1; index >= 0; index--) {
            double progress = Double.valueOf(nutritionsWithProgress[index].nutritionTextView.getText().toString());
            if (!FloatUtils.areFloatsEquals(progress, 0)) {
                right = nutritionsWithProgress[index];
                break;
            }
        }

        if (left == null || right == null) {
            // means that foodstuff has no nutritions
            return;
        }

        Resources resources = context.getResources();
        float cornersRadiusInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, PROGRESSBAR_CORNERS_RADIUS, resources.getDisplayMetrics());
        // if the most left and right lines are the same - make all corners round
        if (left == right) {
            GradientDrawable background = (GradientDrawable) left.progressView.getBackground();
            background.setCornerRadius(cornersRadiusInDp);
        } else {
            GradientDrawable leftBackground = (GradientDrawable) left.progressView.getBackground();
            float[] leftCorners = new float[]{
                    cornersRadiusInDp, cornersRadiusInDp,
                    0, 0,
                    0, 0,
                    cornersRadiusInDp, cornersRadiusInDp};
            leftBackground.setCornerRadii(leftCorners);

            GradientDrawable rightBackground = (GradientDrawable) right.progressView.getBackground();
            float[] rightCorners = new float[]{
                    0, 0,
                    cornersRadiusInDp, cornersRadiusInDp,
                    cornersRadiusInDp, cornersRadiusInDp,
                    0, 0};
            rightBackground.setCornerRadii(rightCorners);
        }
    }

    private void setNutritionValue(TextView nutritionTextView, double nutritionValue) {
        // Заменяем запятую на точку, потому что когда делаешь getString()
        // вместо точки образуется запятая из-за локали,
        // а Double.valueOf() не может распарсить строку с запятой
        nutritionTextView.setText(context.getString(R.string.one_digit_precision_float,
                nutritionValue).replace(',', '.'));
    }

    private void setNutritionProgress(View nutritionView, double progress) {
        nutritionView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) progress));
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
                    Double.valueOf(proteinTextView.getText().toString()),
                    Double.valueOf(fatsTextView.getText().toString()),
                    Double.valueOf(carbsTextView.getText().toString()),
                    Double.valueOf(caloriesTextView.getText().toString()));
            listener.onClick(clickedFoodstuff);
        });
    }
}
