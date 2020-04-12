package korablique.recipecalculator.ui.bucketlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.DishNutritionCalculator;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Recipe;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.TextWatcherAfterTextChangedAdapter;

public class SaveRecipeDialog extends BaseBottomDialog {
    public static String SELECTED_INGREDIENTS = "SELECTED_INGREDIENTS";
    public static String RESULT_WEIGHT = "RESULT_WEIGHT";
    public static String COMMENT = "COMMENT";
    public static String SAVE_DISH = "SAVE_DISH";
    private OnSaveDishButtonClickListener listener;
    private Button saveDishButton;

    public interface OnSaveDishButtonClickListener {
        void onClick(Recipe recipe);
    }

    @Override
    protected boolean shouldOpenKeyboardWhenShown() {
        return true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.save_as_dish_dialog, null);
        saveDishButton = dialogView.findViewById(R.id.save_button);
        if (listener != null) {
            saveDishButton.setOnClickListener(v -> {
                listener.onClick(extractRecipe());
            });
        }

        EditText dishNameView = dialogView.findViewById(R.id.dish_name_edit_text);
        dishNameView.addTextChangedListener(new TextWatcherAfterTextChangedAdapter(editable -> {
            updateSaveDishButtonEnability(dialogView);
        }));
        updateSaveDishButtonEnability(dialogView);
        return dialogView;
    }


    private void updateSaveDishButtonEnability(View dialogView) {
        EditText dishNameView = dialogView.findViewById(R.id.dish_name_edit_text);
        boolean isDishNameFilled = !dishNameView.getText().toString().isEmpty();
        saveDishButton.setEnabled(isDishNameFilled);
    }

    public static SaveRecipeDialog showDialog(
            FragmentActivity activity,
            List<Ingredient> ingredients,
            float resultWeight,
            String comment) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(SELECTED_INGREDIENTS, new ArrayList<>(ingredients));
        bundle.putFloat(RESULT_WEIGHT, resultWeight);
        bundle.putString(COMMENT, comment);
        SaveRecipeDialog dialog = new SaveRecipeDialog();
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(), SAVE_DISH);
        return dialog;
    }

    public void setOnSaveRecipeButtonClickListener(OnSaveDishButtonClickListener listener) {
        View dialogView = getView();
        if (dialogView != null) {
            dialogView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(extractRecipe());
                }
            });
        } else {
            this.listener = listener;
        }
    }

    /**
     * Создает рецепт, рассчитанный из ингридиентов, выбранных в BucketListActivity
     */
    private Recipe extractRecipe() {
        ArrayList<Ingredient> ingredients = getArguments().getParcelableArrayList(SELECTED_INGREDIENTS);
        float resultWeight = getArguments().getFloat(RESULT_WEIGHT);
        String comment = getArguments().getString(COMMENT);
        Nutrition nutrition = DishNutritionCalculator.calculateIngredients(ingredients, resultWeight);
        EditText nameEditText = getView().findViewById(R.id.dish_name_edit_text);
        String name = nameEditText.getText().toString();

        Foodstuff foodstuff = Foodstuff.withName(name).withNutrition(nutrition);
        return Recipe.create(foodstuff, ingredients, resultWeight, comment);
    }

    @Nullable
    public static SaveRecipeDialog findDialog(FragmentActivity activity) {
        return (SaveRecipeDialog) activity.getSupportFragmentManager().findFragmentByTag(SAVE_DISH);
    }
}
