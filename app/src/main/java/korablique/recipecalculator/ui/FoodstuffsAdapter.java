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

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;

/**
 * @param <T> must be either Foodstuff or WeighedFoodstuff. This is enforced by the constructor
 * being private and 2 public factory methods playing the role of the constructor.
 */
public class FoodstuffsAdapter<T> extends RecyclerView.Adapter<MyViewHolder> {
    public interface Observer<OT> {
        void onItemClicked(OT foodstuff, int displayedPosition);
        void onItemsCountChanged(int count);
    }
    private Context context;
    private List<T> allFoodstuffs = new ArrayList<>();
    private List<T> filteredFoodstuffs = new ArrayList<>();
    private Observer<T> observer;
    private String memorizedFilter;

    public static FoodstuffsAdapter<Foodstuff> forFoodstuffs(Context context, Observer observer) {
        return new FoodstuffsAdapter<>(context, observer);
    }

    public static FoodstuffsAdapter<WeightedFoodstuff> forWeighedFoodstuffs(
            Context context, Observer observer) {
        return new FoodstuffsAdapter<>(context, observer);
    }

    private FoodstuffsAdapter(Context context, Observer observer) {
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
        final T foodstuff = getItem(displayedPosition);
        if (foodstuff instanceof WeightedFoodstuff) {
            WeightedFoodstuff weightedFoodstuff = (WeightedFoodstuff) foodstuff;
            double weight = weightedFoodstuff.getWeight();
            setTextViewText(item, R.id.name, context.getString(
                    R.string.foodstuff_name_and_weight, weightedFoodstuff.getName(), weightedFoodstuff.getWeight()));
            setNutritions(
                    item,
                    weightedFoodstuff.getProtein() * weight * 0.01,
                    weightedFoodstuff.getFats() * weight * 0.01,
                    weightedFoodstuff.getCarbs() * weight * 0.01,
                    weightedFoodstuff.getCalories() * weight * 0.01);
        } else {
            Foodstuff plainFoodstuff = (Foodstuff) foodstuff;
            setTextViewText(item, R.id.name, plainFoodstuff.getName());
            setNutritions(
                    item,
                    plainFoodstuff.getProtein(),
                    plainFoodstuff.getFats(),
                    plainFoodstuff.getCarbs(),
                    plainFoodstuff.getCalories());
        }
        item.setOnClickListener((v) -> {
            observer.onItemClicked(foodstuff, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return filteredFoodstuffs.size();
    }

    public void addItems(List<T> foodstuffs) {
        int allFoodstuffsSizeBefore = allFoodstuffs.size();
        allFoodstuffs.addAll(foodstuffs);
        for (int index = 1; index <= foodstuffs.size(); index++) {
            notifyItemInserted(allFoodstuffsSizeBefore + index);
        }
        observer.onItemsCountChanged(allFoodstuffs.size());
        setNameFilter(memorizedFilter);
    }

    public void addItem(T foodstuff) {
        addItems(Collections.singletonList(foodstuff));
    }

    public void deleteItem(int displayedPosition) {
        T deleted = filteredFoodstuffs.get(displayedPosition);
        int indexInAllFoodstuffs = allFoodstuffs.indexOf(deleted);
        filteredFoodstuffs.remove(displayedPosition);
        allFoodstuffs.remove(indexInAllFoodstuffs);
        notifyItemRemoved(displayedPosition);
        observer.onItemsCountChanged(allFoodstuffs.size());
    }

    public void replaceItem(T newFoodstuff, int displayedPosition) {
        T outdated = filteredFoodstuffs.get(displayedPosition);
        int indexInAllFoodstuffs = allFoodstuffs.indexOf(outdated);
        filteredFoodstuffs.set(displayedPosition, newFoodstuff);
        allFoodstuffs.set(indexInAllFoodstuffs, newFoodstuff);
        notifyItemChanged(displayedPosition);
    }

    public T getItem(int displayedPosition) {
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
        for (T genericFoodstuff : allFoodstuffs) {
            Foodstuff foodstuff = asFoodstuff(genericFoodstuff);
            if (foodstuff.getName().toLowerCase().contains(name.toLowerCase())) {
                filteredFoodstuffs.add(genericFoodstuff);
            }
        }
        notifyDataSetChanged();
    }

    private Foodstuff asFoodstuff(T param) {
        if (param instanceof WeightedFoodstuff) {
            return ((WeightedFoodstuff) param).withoutWeight();
        }
        if (param instanceof Foodstuff) {
            return (Foodstuff) param;
        }
        throw new IllegalArgumentException("Type is not supported: " + param.getClass().getName());
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

    private <TextType> void setTextViewText(View parent, int viewId, TextType text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }
}
