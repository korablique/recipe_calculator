package korablique.recipecalculator.ui.nestingadapters;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.MyViewHolder;

public class FoodstuffsAdapterChild extends AdapterChild {
    public interface ClickObserver {
        void onItemClicked(Foodstuff foodstuff, int displayedPosition);
    }
    public static final int VIEW_TYPE_FOODSTUFF = 0;
    private List<Foodstuff> foodstuffs = new ArrayList<>();
    private Context context;
    private ClickObserver clickObserver;

    public FoodstuffsAdapterChild(Context context, ClickObserver clickObserver) {
        super(1);
        this.context = context;
        this.clickObserver = clickObserver;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOODSTUFF) {
            ViewGroup foodstuffView = (ViewGroup) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.new_foodstuff_layout, parent, false);
            return new MyViewHolder(foodstuffView);
        } else {
            throw new IllegalArgumentException("No such view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition) {
        ViewGroup item = ((MyViewHolder) holder).getItem();
        Foodstuff foodstuff = foodstuffs.get(childPosition);
        setTextViewText(item, R.id.new_foodstuff_name, foodstuff.getName());

        setCalories(item, foodstuff.getCalories());

        item.setOnClickListener((v) -> {
            clickObserver.onItemClicked(foodstuff, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return foodstuffs.size();
    }

    @Override
    public int getItemViewType(int childPosition) {
        return VIEW_TYPE_FOODSTUFF;
    }

    public void addItems(List<Foodstuff> items) {
        int allFoodstuffsSizeBefore = foodstuffs.size();
        foodstuffs.addAll(items);
        for (int index = 0; index <= items.size(); index++) {
            for (Observer observer : getObservers()) {
                observer.notifyItemInsertedToChild(allFoodstuffsSizeBefore + index, this);
            }
        }
    }

    private void setTextViewText(View parent, int viewId, String text) {
        ((TextView) parent.findViewById(viewId)).setText(text);
    }

    private void setCalories(View foodstuffView, double calories) {
        setTextViewText(foodstuffView, R.id.calories_block, context.getString(
                R.string.one_digit_precision_float, calories));
    }

    public List<Foodstuff> getItems() {
        return new ArrayList<>(foodstuffs);
    }
}
