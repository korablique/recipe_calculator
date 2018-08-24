package korablique.recipecalculator.ui.bucketlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.DishNutritionCalculator;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class SaveDishDialog extends DialogFragment {
    public static String SELECTED_FOODSTUFFS = "SELECTED_FOODSTUFFS";
    public static String RESULT_WEIGHT = "RESULT_WEIGHT";
    public static String SAVE_DISH = "SAVE_DISH";
    private OnSaveDishButtonClickListener listener;

    public interface OnSaveDishButtonClickListener {
        void onClick(Foodstuff foodstuff);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.save_as_dish_dialog, null);
        if (listener != null) {
            dialogView.findViewById(R.id.save_button).setOnClickListener(v -> {
                listener.onClick(extractFoodstuff());
            });
        }
        return dialogView;
    }

    @Override
    @NonNull public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnShowListener(unused -> {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            dialog.getWindow().setAttributes(layoutParams);
            dialog.getWindow().setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.new_card_background));
        });
        return dialog;
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
