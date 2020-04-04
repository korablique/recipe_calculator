package korablique.recipecalculator.ui.mainactivity.history.pages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.MyViewHolder;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class HistoryAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private static final int VIEW_TYPE_EMPTY_TOP = 0;
    private static final int VIEW_TYPE_FOODSTUFF = 1;
    private static final int VIEW_TYPE_EMPTY_BOTTOM = 2;

    public interface Observer {
        void onItemClicked(HistoryEntry historyEntry);
    }
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private Context context;
    private HistoryViewHoldersPool viewHoldersPool;
    @Nullable
    private Observer onItemClickObserver;

    private boolean destroyed;
    private Set<View> aliveViews = Collections.newSetFromMap(new WeakHashMap<>());

    public HistoryAdapter(Context context, HistoryViewHoldersPool viewHoldersPool) {
        this.context = context;
        this.viewHoldersPool = viewHoldersPool;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        checkNotDestroyed();
        View view;
        switch (viewType) {
            case VIEW_TYPE_EMPTY_TOP:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.history_recycler_view_empty_top, parent, false);
                return new MyViewHolder(view);
            case VIEW_TYPE_FOODSTUFF:
                view = viewHoldersPool.take();
                aliveViews.add(view);
                return new MyViewHolder(view);
            case VIEW_TYPE_EMPTY_BOTTOM:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.history_recycler_view_empty_bottom, parent, false);
                return new MyViewHolder(view);
            default:
                throw new IllegalStateException("Unknown view type: " + viewType);
        }
    }

    private void checkNotDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("The adapter is already destroyed");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        checkNotDestroyed();
        if (position == 0 || position == getItemCount()-1) {
            // Empty item is not bound to any data
            return;
        }
        View item = holder.getItem();
        TextView foodstuffNameWithWeightView = item.findViewById(R.id.foodstuff_name_and_weight);
        TextView proteinView = item.findViewById(R.id.protein);
        TextView fatsView = item.findViewById(R.id.fats);
        TextView carbsView = item.findViewById(R.id.carbs);
        TextView caloriesView = item.findViewById(R.id.calories);

        HistoryEntry entry = historyEntries.get(position - 1); // First item is empty space
        WeightedFoodstuff foodstuff = entry.getFoodstuff();
        foodstuffNameWithWeightView.setText(context.getString(R.string.foodstuff_name_and_weight,
                foodstuff.getName(), foodstuff.getWeight()));
        double foodstuffWeight = foodstuff.getWeight();
        proteinView.setText(toDecimalString(foodstuff.getProtein() * foodstuffWeight * 0.01));
        fatsView.setText(toDecimalString(foodstuff.getFats() * foodstuffWeight * 0.01));
        carbsView.setText(toDecimalString(foodstuff.getCarbs() * foodstuffWeight * 0.01));
        caloriesView.setText(toDecimalString(foodstuff.getCalories() * foodstuffWeight * 0.01));

        item.setOnClickListener(v -> {
            if (onItemClickObserver != null) {
                onItemClickObserver.onItemClicked(entry); // First item is empty space
            }
        });
    }

    @Override
    public int getItemCount() {
        checkNotDestroyed();
        return historyEntries.size() + 2; // First and last items are empty spaces
    }

    @Override
    public int getItemViewType(int position) {
        checkNotDestroyed();
        if (position == 0) {
            return VIEW_TYPE_EMPTY_TOP;
        } else if (position == getItemCount() - 1) {
            return VIEW_TYPE_EMPTY_BOTTOM;
        } else {
            return VIEW_TYPE_FOODSTUFF;
        }
    }

    public List<HistoryEntry> getItems() {
        return Collections.unmodifiableList(historyEntries);
    }

    public void addItem(HistoryEntry historyEntry) {
        historyEntries.add(historyEntry);
        // size() instead of size()-1 - first item is empty space
        notifyItemInserted(historyEntries.size());
    }

    public void addItems(List<HistoryEntry> historyEntries) {
        for (HistoryEntry entry : historyEntries) {
            addItem(entry);
        }
    }

    /**
     * @param foodstuff foodstuff using to find appropriate history entry
     * @return removing history entry object or null
     */
    public HistoryEntry removeItem(WeightedFoodstuff foodstuff) {
        if (foodstuff.getId() == -1) {
            throw new IllegalArgumentException("Foodstuff has no id");
        }
        for (int index = 0; index < historyEntries.size(); index++) {
            WeightedFoodstuff f = historyEntries.get(index).getFoodstuff();
            if (foodstuff.equals(f)) {
                HistoryEntry removingEntry = historyEntries.get(index);
                historyEntries.remove(index);
                notifyItemRemoved(index + 1); // First item is empty space
                return removingEntry;
            }
        }
        return null;
    }

    /**
     * @param newFoodstuff foodstuff with changed weight
     * @return replaced item's id or -1 if item not found
     */
    public long replaceItem(WeightedFoodstuff oldFoodstuff, WeightedFoodstuff newFoodstuff) {
        if (newFoodstuff.getId() == -1) {
            throw new IllegalArgumentException("Foodstuff has no id");
        }
        for (int index = 0; index < historyEntries.size(); index++) {
            WeightedFoodstuff foodstuff = historyEntries.get(index).getFoodstuff();
            if (foodstuff.equals(oldFoodstuff)) {
                HistoryEntry oldEntry = historyEntries.get(index);
                historyEntries.set(index, new HistoryEntry(oldEntry.getHistoryId(), newFoodstuff, oldEntry.getTime()));
                notifyItemChanged(index + 1); // First item is empty space
                return oldEntry.getHistoryId();
            }
        }
        return -1;
    }

    public void clear() {
        historyEntries.clear();
        notifyDataSetChanged();
    }

    public void setOnItemClickObserver(@Nullable Observer observer) {
        onItemClickObserver = observer;
    }

    public void destroy() {
        checkNotDestroyed();
        destroyed = true;
        viewHoldersPool.put(aliveViews);
        aliveViews.clear();
    }

    @Override
    public long getItemId(int position) {
        // First and last items are empty spaces
        if (position == 0) {
            return -1;
        } else if (position == getItemCount() - 1) {
            return -2;
        } else {
            return historyEntries.get(position - 1).getHistoryId();
        }
    }
}
