package korablique.recipecalculator.ui;

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

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.R;

public class FoodstuffsAdapter extends RecyclerView.Adapter<MyViewHolder> {
    public interface Observer {
        void onItemClicked(Foodstuff foodstuff, int displayedPosition);
        void onItemsCountChanged(int count);
    }
    private Context context;
    private List<Foodstuff> allFoodstuffs = new ArrayList<>();
    private List<Foodstuff> filteredFoodstuffs = new ArrayList<>();
    private Observer observer;
    private String memorizedFilter;

    public FoodstuffsAdapter(Context context, Observer observer) {
        this.context = context;
        this.observer = observer;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.foodstuff_layout, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int displayedPosition) {
        ViewGroup item = holder.getItem();
        final Foodstuff foodstuff = getItem(displayedPosition);
        double weight = foodstuff.getWeight();
        if (weight != -1) {
            setTextViewText(item, R.id.name, context.getString(
                    R.string.foodstuff_name_and_weight, foodstuff.getName(), foodstuff.getWeight()));
            setNutritions(
                    item,
                    foodstuff.getProtein() * weight * 0.01,
                    foodstuff.getFats() * weight * 0.01,
                    foodstuff.getCarbs() * weight * 0.01,
                    foodstuff.getCalories() * weight * 0.01);
        } else {
            setTextViewText(item, R.id.name, foodstuff.getName());
            setNutritions(
                    item,
                    foodstuff.getProtein(),
                    foodstuff.getFats(),
                    foodstuff.getCarbs(),
                    foodstuff.getCalories());
        }
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                observer.onItemClicked(foodstuff, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredFoodstuffs.size();
    }

    public void addItems(List<Foodstuff> foodstuffs) {
        int allFoodstuffsSizeBefore = allFoodstuffs.size();
        allFoodstuffs.addAll(foodstuffs);
        for (int index = 1; index <= foodstuffs.size(); index++) {
            notifyItemInserted(allFoodstuffsSizeBefore + index);
        }
        observer.onItemsCountChanged(allFoodstuffs.size());
        setNameFilter(memorizedFilter);
    }

    public void addItem(Foodstuff foodstuff) {
        addItems(Collections.singletonList(foodstuff));
    }

    public void deleteItem(int displayedPosition) {
        Foodstuff deleted = filteredFoodstuffs.get(displayedPosition);
        int indexInAllFoodstuffs = allFoodstuffs.indexOf(deleted);
        filteredFoodstuffs.remove(displayedPosition);
        allFoodstuffs.remove(indexInAllFoodstuffs);
        notifyItemRemoved(displayedPosition);
        observer.onItemsCountChanged(allFoodstuffs.size());
    }

    public void replaceItem(Foodstuff newFoodstuff, int displayedPosition) {
        Foodstuff outdated = filteredFoodstuffs.get(displayedPosition);
        int indexInAllFoodstuffs = allFoodstuffs.indexOf(outdated);
        filteredFoodstuffs.set(displayedPosition, newFoodstuff);
        allFoodstuffs.set(indexInAllFoodstuffs, newFoodstuff);
        notifyItemChanged(displayedPosition);
    }

    public Foodstuff getItem(int displayedPosition) {
        return filteredFoodstuffs.get(displayedPosition);
    }

    public void setNameFilter(String name) {
        if (memorizedFilter == null && name == null) {
            // Чтобы в случае отсутствия фильтра эта операция заканчивалась очень быстро, без итерирования
            filteredFoodstuffs.clear();
            filteredFoodstuffs.addAll(allFoodstuffs);
            return;
        }
        memorizedFilter = name;
        filteredFoodstuffs.clear();
        for (Foodstuff foodstuff : allFoodstuffs) {
            if (foodstuff.getName().toLowerCase().contains(name.toLowerCase())) {
                filteredFoodstuffs.add(foodstuff);
            }
        }
        notifyDataSetChanged();
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

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }
}
