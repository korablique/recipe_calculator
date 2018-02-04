package korablique.recipecalculator.ui.mainscreen;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.FoodstuffViewHolder;

public class TopAdapterChild extends AdapterChild {
    private List<Foodstuff> topFoodstuffs = new ArrayList<>();
    private Context context;

    public TopAdapterChild(Context context) {
        super(1);
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout topFoodstuffView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.top_foodstuff_layout, parent, false);
        return new FoodstuffViewHolder(topFoodstuffView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LinearLayout item = ((FoodstuffViewHolder) holder).getItem();
        Foodstuff foodstuff = topFoodstuffs.get(position);
        setTextViewText(item, R.id.name, foodstuff.getName());
        setNutritions(
                item,
                foodstuff.getProtein(),
                foodstuff.getFats(),
                foodstuff.getCarbs(),
                foodstuff.getCalories());
    }

    @Override
    public int getItemCount() {
        return topFoodstuffs.size();
    }

    @Override
    public int getItemViewType(int childPosition) {
        return 0;
    }

    public void addItems(List<Foodstuff> foodstuffs) {
        int allFoodstuffsSizeBefore = topFoodstuffs.size();
        topFoodstuffs.addAll(foodstuffs);

        for (int index = 0; index < foodstuffs.size(); index++) {
            for (Observer observer : getObservers()) {
                observer.notifyItemInsertedToChild(allFoodstuffsSizeBefore + index, this);
            }
        }
    }

    public void addItem(Foodstuff foodstuff) {
        addItems(Collections.singletonList(foodstuff));
    }

    private void setTextViewText(View parent, int viewId, String text) {
        ((TextView) parent.findViewById(viewId)).setText(text);
    }

    private void setNutritions(View foodstuffView, double protein, double fats, double carbs, double calories) {
        setTextViewText(foodstuffView, R.id.protein, context.getString(
                R.string.one_digit_precision_float, protein));
        setTextViewText(foodstuffView, R.id.fats, context.getString(
                R.string.one_digit_precision_float, fats));
        setTextViewText(foodstuffView, R.id.carbs, context.getString(
                R.string.one_digit_precision_float, carbs));
        setTextViewText(foodstuffView, R.id.calories, context.getString(
                R.string.one_digit_precision_float, calories));
    }
}
