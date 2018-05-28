package korablique.recipecalculator.ui;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import korablique.recipecalculator.FloatUtils;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.card.NewCard;

public class NutritionProgressWithNumbersWrapper {
    private static final int PROGRESSBAR_CORNERS_RADIUS = 4;
    private ViewGroup layout;
    private Context context;
    private TextView proteinTextView;
    private TextView fatsTextView;
    private TextView carbsTextView;
    private TextView caloriesTextView;

    private static class NutritionWithProgress {
        TextView nutritionTextView;
        View progressView;

        public NutritionWithProgress(TextView nutritionTextView, View progressView) {
            this.nutritionTextView = nutritionTextView;
            this.progressView = progressView;
        }
    }

    public NutritionProgressWithNumbersWrapper(Context context, ViewGroup layout) {
        this.layout = layout;
        this.context = context;

        proteinTextView = this.layout.findViewById(R.id.protein_layout).findViewById(R.id.nutrition_text_view);
        fatsTextView = this.layout.findViewById(R.id.fats_layout).findViewById(R.id.nutrition_text_view);
        carbsTextView = this.layout.findViewById(R.id.carbs_layout).findViewById(R.id.nutrition_text_view);
        caloriesTextView = this.layout.findViewById(R.id.calories_layout).findViewById(R.id.nutrition_text_view);

        setNutritionTable(R.id.protein_layout, R.string.protein, R.drawable.new_card_protein_icon);
        setNutritionTable(R.id.fats_layout, R.string.fats, R.drawable.new_card_fats_icon);
        setNutritionTable(R.id.carbs_layout, R.string.carbs, R.drawable.new_card_carbs_icon);
        setNutritionTable(R.id.calories_layout, R.string.calories_per_100_g, R.drawable.invisible_drawable);
    }

    // только задает цвета кружкам и названия в шапке
    private void setNutritionTable(@IdRes int nutritionLayout, @StringRes int nutritionName, @DrawableRes int drawable) {
        ViewGroup layout = this.layout.findViewById(nutritionLayout);
        TextView header = layout.findViewById(R.id.nutrition_name);
        header.setText(nutritionName);
        View coloredCircle = layout.findViewById(R.id.colored_circle);
        coloredCircle.setBackground(this.layout.getResources().getDrawable(drawable));
    }

    public void setNutrition(Nutrition nutrition) {
        setNutritionValue(proteinTextView, nutrition.getProtein());
        setNutritionValue(fatsTextView, nutrition.getFats());
        setNutritionValue(carbsTextView, nutrition.getCarbs());
        setNutritionValue(caloriesTextView, nutrition.getCalories());

        // set progress in progressbar
        View proteinProgress = layout.findViewById(R.id.protein_progress);
        setNutritionProgress(proteinProgress, nutrition.getProtein());
        View fatsProgress = layout.findViewById(R.id.fats_progress);
        setNutritionProgress(fatsProgress, nutrition.getFats());
        View carbsProgress = layout.findViewById(R.id.carbs_progress);
        setNutritionProgress(carbsProgress, nutrition.getCarbs());
        View nothing = layout.findViewById(R.id.nothing_progress);
        setNutritionProgress(nothing, 100 - nutrition.getProtein() - nutrition.getFats() - nutrition.getCarbs());

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

    public double getProteinValue() {
        return Double.valueOf(proteinTextView.getText().toString());
    }

    public double getFatsValue() {
        return Double.valueOf(fatsTextView.getText().toString());
    }

    public double getCarbsValue() {
        return Double.valueOf(carbsTextView.getText().toString());
    }

    public double getCaloriesValue() {
        return Double.valueOf(caloriesTextView.getText().toString());
    }
}
