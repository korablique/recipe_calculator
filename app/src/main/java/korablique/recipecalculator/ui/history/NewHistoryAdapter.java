package korablique.recipecalculator.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.MyViewHolder;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class NewHistoryAdapter extends RecyclerView.Adapter<MyViewHolder> {
    public interface Observer {
        void onItemClicked(HistoryEntry historyEntry, int displayedPosition);
    }
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private Context context;
    @Nullable
    private Observer onItemClickObserver;

    public NewHistoryAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewGroup historyItemView = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_layout, parent, false);
        return new MyViewHolder(historyItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ViewGroup item = holder.getItem();
        TextView foodstuffNameWithWeightView = item.findViewById(R.id.foodstuff_name_and_weight);
        TextView proteinView = item.findViewById(R.id.protein);
        TextView fatsView = item.findViewById(R.id.fats);
        TextView carbsView = item.findViewById(R.id.carbs);
        TextView caloriesView = item.findViewById(R.id.calories);

        HistoryEntry entry = historyEntries.get(position);
        WeightedFoodstuff foodstuff = entry.getFoodstuff();
        foodstuffNameWithWeightView.setText(context.getString(R.string.foodstuff_name_and_weight,
                foodstuff.getName(), foodstuff.getWeight()));
        double foodstuffWeight = foodstuff.getWeight();
        proteinView.setText(toDecimalString(foodstuff.getProtein() * foodstuffWeight * 0.01));
        fatsView.setText(toDecimalString(foodstuff.getFats() * foodstuffWeight * 0.01));
        carbsView.setText(toDecimalString(foodstuff.getCarbs() * foodstuffWeight * 0.01));
        caloriesView.setText(toDecimalString(foodstuff.getCalories() * foodstuffWeight * 0.01));

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickObserver != null) {
                    onItemClickObserver.onItemClicked(entry, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyEntries.size();
    }

    public HistoryEntry getItem(int position) {
        return historyEntries.get(position);
    }

    public void addItem(HistoryEntry historyEntry) {
        historyEntries.add(historyEntry);
        notifyItemInserted(historyEntries.size() - 1);
    }

    public void addItems(List<HistoryEntry> historyEntries) {
        for (HistoryEntry entry : historyEntries) {
            addItem(entry);
        }
    }

    public void replaceItem(WeightedFoodstuff foodstuff, int position) {
        HistoryEntry oldEntry = historyEntries.get(position);
        historyEntries.set(position, new HistoryEntry(oldEntry.getHistoryId(), foodstuff, oldEntry.getTime()));
        notifyItemChanged(position);
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
            if (foodstuff.getId() == f.getId()) {
                HistoryEntry removingEntry = historyEntries.get(index);
                historyEntries.remove(index);
                notifyItemRemoved(index);
                return removingEntry;
            }
        }
        return null;
    }

    /**
     * @param newFoodstuff foodstuff with changed weight
     * @return replaced item's id or -1 if item not found
     */
    public long replaceItem(WeightedFoodstuff newFoodstuff) {
        if (newFoodstuff.getId() == -1) {
            throw new IllegalArgumentException("Foodstuff has no id");
        }
        for (int index = 0; index < historyEntries.size(); index++) {
            WeightedFoodstuff foodstuff = historyEntries.get(index).getFoodstuff();
            if (newFoodstuff.getId() == foodstuff.getId()) {
                HistoryEntry oldEntry = historyEntries.get(index);
                historyEntries.set(index, new HistoryEntry(oldEntry.getHistoryId(), newFoodstuff, oldEntry.getTime()));
                notifyItemChanged(index);
                return oldEntry.getHistoryId();
            }
        }
        return -1;
    }

    public void setOnItemClickObserver(@Nullable Observer observer) {
        onItemClickObserver = observer;
    }
}
