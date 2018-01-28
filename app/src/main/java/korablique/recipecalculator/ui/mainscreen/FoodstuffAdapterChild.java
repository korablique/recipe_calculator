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

public class FoodstuffAdapterChild implements AdapterChild {
    private List<AdapterChildObserver> observers = new ArrayList<>();
    private List<Foodstuff> allFoodstuffs = new ArrayList<>();
    private Context context;

    public FoodstuffAdapterChild(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout topFoodstuffView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.foodstuff_layout, parent, false);
        return new FoodstuffViewHolder(topFoodstuffView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LinearLayout item = ((FoodstuffViewHolder) holder).getItem();
        Foodstuff foodstuff = allFoodstuffs.get(position);
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
        return allFoodstuffs.size();
    }

    @Override
    public int getItemViewType(int childPosition) {
        return 0;
    }

    @Override
    public int getItemViewTypesCount() {
        return 2;
    }

    @Override
    public void addObserver(AdapterChildObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(AdapterChildObserver observer) {
        observers.remove(observer);
    }

    public void addItems(List<Foodstuff> foodstuffs) {
        int allFoodstuffsSizeBefore = allFoodstuffs.size();
        allFoodstuffs.addAll(foodstuffs);
        for (int index = 0; index < foodstuffs.size(); index++) {
            for (AdapterChildObserver observer : observers) {
                observer.notifyItemInsertedToChild(allFoodstuffsSizeBefore + index, this);
            }
        }
    }

    public void addItem(Foodstuff foodstuff) {
        addItems(Collections.singletonList(foodstuff));
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
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
