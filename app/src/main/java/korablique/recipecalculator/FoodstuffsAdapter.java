package korablique.recipecalculator;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FoodstuffsAdapter extends RecyclerView.Adapter<FoodstuffViewHolder> {
    public interface Observer {
        void onItemClicked(Foodstuff foodstuff, int position);
        void onItemsCountChanged(int count);
    }
    private List<Foodstuff> foodstuffs = new ArrayList<>();
    private Observer observer;
    private boolean shouldHideAllWeights;

    public FoodstuffsAdapter(Observer observer) {
        this.observer = observer;
    }

    @Override
    public FoodstuffViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.foodstuff_layout, parent, false);
        return new FoodstuffViewHolder(item);
    }

    @Override
    public void onBindViewHolder(final FoodstuffViewHolder holder, int position) {
        LinearLayout item = holder.getItem();
        final Foodstuff foodstuff = foodstuffs.get(position);
        ((TextView) item.findViewById(R.id.name)).setText(foodstuff.getName());
        ((TextView) item.findViewById(R.id.weight)).setText(String.valueOf(foodstuff.getWeight()));
        ((TextView) item.findViewById(R.id.protein)).setText(String.valueOf(foodstuff.getProtein()));
        ((TextView) item.findViewById(R.id.fats)).setText(String.valueOf(foodstuff.getFats()));
        ((TextView) item.findViewById(R.id.carbs)).setText(String.valueOf(foodstuff.getCarbs()));
        ((TextView) item.findViewById(R.id.calories)).setText(String.valueOf(foodstuff.getCalories()));
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                observer.onItemClicked(foodstuff, holder.getAdapterPosition());
            }
        });
        View weightColumnName = item.findViewById(R.id.column_name_weight);
        View weightView = item.findViewById(R.id.weight);
        if (shouldHideAllWeights) {
            weightColumnName.setVisibility(View.GONE);
            weightView.setVisibility(View.GONE);
        } else {
            weightColumnName.setVisibility(View.VISIBLE);
            weightView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return foodstuffs.size();
    }

    public void addItem(Foodstuff foodstuff) {
        foodstuffs.add(foodstuff);
        notifyItemInserted(foodstuffs.size() - 1);
        observer.onItemsCountChanged(foodstuffs.size());
    }

    public void deleteItem(int position) {
        foodstuffs.remove(position);
        notifyItemRemoved(position);
        observer.onItemsCountChanged(foodstuffs.size());
    }

    public void replaceItem(Foodstuff newFoodstuff, int position) {
        foodstuffs.set(position, newFoodstuff);
        notifyItemChanged(position);
    }

    public Foodstuff getItem(int position) {
        return foodstuffs.get(position);
    }

    public void hideWeight() {
        shouldHideAllWeights = true;
        notifyDataSetChanged();
    }
}
