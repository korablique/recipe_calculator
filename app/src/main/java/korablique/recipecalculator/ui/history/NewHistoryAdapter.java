package korablique.recipecalculator.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.MyViewHolder;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class NewHistoryAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private Context context;

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
}
