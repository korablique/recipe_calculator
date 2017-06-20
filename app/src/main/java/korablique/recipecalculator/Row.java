package korablique.recipecalculator;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Row {
    private View rowLayout;
    private TextView nameTextView;
    private TextView weightTextView;
    private TextView proteinTextView;
    private TextView fatsTextView;
    private TextView carbsTextView;
    private TextView caloriesTextView;

    public Row(Activity activity, ViewGroup parentLayout, LinearLayout newRowLayout) {
        if (newRowLayout == null) {
            rowLayout = LayoutInflater.from(activity).inflate(R.layout.foodstuff_layout, null);
            parentLayout.addView(rowLayout);
        } else {
            rowLayout = newRowLayout;
        }

        nameTextView = (TextView) rowLayout.findViewById(R.id.name);
        weightTextView = (TextView) rowLayout.findViewById(R.id.weight);
        proteinTextView = (TextView) rowLayout.findViewById(R.id.protein);
        fatsTextView = (TextView) rowLayout.findViewById(R.id.fats);
        carbsTextView = (TextView) rowLayout.findViewById(R.id.carbs);
        caloriesTextView = (TextView) rowLayout.findViewById(R.id.calories);
    }

    public TextView getNameTextView() {
        return nameTextView;
    }

    public TextView getWeightTextView() {
        return weightTextView;
    }

    public TextView getProteinTextView() {
        return proteinTextView;
    }

    public TextView getFatsTextView() {
        return fatsTextView;
    }

    public TextView getCarbsTextView() {
        return carbsTextView;
    }

    public TextView getCaloriesTextView() {
        return caloriesTextView;
    }
}
