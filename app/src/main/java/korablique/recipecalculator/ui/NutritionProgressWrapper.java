package korablique.recipecalculator.ui;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import korablique.recipecalculator.FloatUtils;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Nutrition;

public class NutritionProgressWrapper {
    private static final int PROGRESSBAR_CORNERS_RADIUS = 4;
    private ViewGroup layout;
    private Context context;

    private static class NutritionWithProgress {
        double nutritionValue;
        View progressView;

        public NutritionWithProgress(double nutritionValue, View progressView) {
            this.nutritionValue = nutritionValue;
            this.progressView = progressView;
        }
    }

    public NutritionProgressWrapper(Context context, ViewGroup layout) {
        this.layout = layout;
        this.context = context;
    }

    public void setNutrition(double protein, double fats, double carbs) {
        // set progress in progressbar
        View proteinProgress = layout.findViewById(R.id.protein_progress);
        setNutritionProgress(proteinProgress, protein);
        View fatsProgress = layout.findViewById(R.id.fats_progress);
        setNutritionProgress(fatsProgress, fats);
        View carbsProgress = layout.findViewById(R.id.carbs_progress);
        setNutritionProgress(carbsProgress, carbs);
        View nothing = layout.findViewById(R.id.nothing_progress);
        setNutritionProgress(nothing, 100 - protein - fats - carbs);

        NutritionWithProgress[] nutritionsWithProgress = new NutritionWithProgress[]{
                new NutritionWithProgress(protein, proteinProgress),
                new NutritionWithProgress(fats, fatsProgress),
                new NutritionWithProgress(carbs, carbsProgress)};
        roundCorners(nutritionsWithProgress);
    }

    public void setNutrition(Nutrition nutrition) {
        setNutrition(nutrition.getProtein(), nutrition.getFats(), nutrition.getCarbs());
    }

    private void roundCorners(NutritionWithProgress[] nutritionsWithProgress) {
        // find the most left line
        NutritionWithProgress left = null;
        for (int index = 0; index < nutritionsWithProgress.length; index++) {
            double progress = nutritionsWithProgress[index].nutritionValue;
            if (!FloatUtils.areFloatsEquals(progress, 0)) {
                left = nutritionsWithProgress[index];
                break;
            }
        }
        // find the most right line
        NutritionWithProgress right = null;
        for (int index = nutritionsWithProgress.length - 1; index >= 0; index--) {
            double progress = nutritionsWithProgress[index].nutritionValue;
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

    private void setNutritionProgress(View nutritionView, double progress) {
        nutritionView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) progress));
    }
}
