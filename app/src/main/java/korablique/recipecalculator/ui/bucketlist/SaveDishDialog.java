package korablique.recipecalculator.ui.bucketlist;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.DishNutritionCalculator;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.TextWatcherAfterTextChangedAdapter;

public class SaveDishDialog extends BaseBottomDialog {
    public static String SELECTED_FOODSTUFFS = "SELECTED_FOODSTUFFS";
    public static String RESULT_WEIGHT = "RESULT_WEIGHT";
    public static String SAVE_DISH = "SAVE_DISH";
    private OnSaveDishButtonClickListener listener;
    private Button saveDishButton;

    public interface OnSaveDishButtonClickListener {
        void onClick(Foodstuff foodstuff);
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
                listener.onClick(extractFoodstuff());
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

    public static SaveDishDialog showDialog(FragmentActivity activity, List<WeightedFoodstuff> foodstuffs, double resultWeight) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(SELECTED_FOODSTUFFS, new ArrayList<>(foodstuffs));
        bundle.putDouble(RESULT_WEIGHT, resultWeight);
        SaveDishDialog dialog = new SaveDishDialog();
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(), SAVE_DISH);
        return dialog;
    }

    public void setOnSaveDishButtonClickListener(OnSaveDishButtonClickListener listener) {
        View dialogView = getView();
        if (dialogView != null) {
            dialogView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(extractFoodstuff());
                }
            });
        } else {
            this.listener = listener;
        }
    }

    /**
     * Создает фудстафф для блюда, рассчитанного из ингридиентов, выбранных в BucketListActivity
     */
    private Foodstuff extractFoodstuff() {
        ArrayList<WeightedFoodstuff> savedFoodstuffs = getArguments().getParcelableArrayList(SELECTED_FOODSTUFFS);
        double resultWeight = getArguments().getDouble(RESULT_WEIGHT);
        Nutrition nutrition = DishNutritionCalculator.calculate(savedFoodstuffs, resultWeight);
        EditText nameEditText = getView().findViewById(R.id.dish_name_edit_text);
        String name = nameEditText.getText().toString();
        return Foodstuff.withName(name).withNutrition(nutrition);
    }

    @Nullable
    public static SaveDishDialog findDialog(FragmentActivity activity) {
        return (SaveDishDialog) activity.getSupportFragmentManager().findFragmentByTag(SAVE_DISH);
    }
}
