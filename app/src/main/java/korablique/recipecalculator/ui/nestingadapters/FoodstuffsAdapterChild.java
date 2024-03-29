package korablique.recipecalculator.ui.nestingadapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.MyViewHolder;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class FoodstuffsAdapterChild extends AdapterChild {
    public interface ClickObserver {
        void onItemClicked(Foodstuff foodstuff, int displayedPosition);
    }
    public interface LongClickObserver {
        boolean onItemClicked(Foodstuff foodstuff, int displayedPosition, View view);
        LongClickObserver EMPTY = new LongClickObserver() {
            @Override
            public boolean onItemClicked(Foodstuff foodstuff, int displayedPosition, View view) {
                return false;
            }
        };
    }
    protected List<Foodstuff> foodstuffs = new ArrayList<>();
    private ClickObserver clickObserver;
    private LongClickObserver longClickObserver;

    public FoodstuffsAdapterChild(ClickObserver clickObserver, LongClickObserver longClickObserver) {
        super(1);
        this.clickObserver = clickObserver;
        this.longClickObserver = longClickObserver;
    }

    public FoodstuffsAdapterChild(ClickObserver clickObserver) {
        this(clickObserver, LongClickObserver.EMPTY);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup foodstuffView = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.new_foodstuff_layout, parent, false);
        return new MyViewHolder(foodstuffView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition) {
        View item = ((MyViewHolder) holder).getItem();
        Foodstuff foodstuff = foodstuffs.get(childPosition);
        setTextViewText(item, R.id.name, foodstuff.getName());

        setCalories(item, foodstuff.getCalories());

        item.setOnClickListener((v) -> {
            clickObserver.onItemClicked(foodstuff, holder.getAdapterPosition());
        });
        item.setOnLongClickListener((v) -> {
            return longClickObserver.onItemClicked(foodstuff, holder.getAdapterPosition(), item);
        });
    }

    @Override
    public int getItemCount() {
        return foodstuffs.size();
    }

    @Override
    public int getItemViewType(int childPosition) {
        return 0;
    }

    public void addItem(Foodstuff foodstuff, int index) {
        foodstuffs.add(index, foodstuff);
        for (Observer observer : getObservers()) {
            observer.notifyItemInsertedToChild(index, this);
        }
    }

    public void addItems(List<Foodstuff> items) {
        int allFoodstuffsSizeBefore = foodstuffs.size();
        foodstuffs.addAll(items);
        for (int index = 0; index < items.size(); index++) {
            for (Observer observer : getObservers()) {
                observer.notifyItemInsertedToChild(allFoodstuffsSizeBefore + index, this);
            }
        }
    }

    public void removeItem(Foodstuff foodstuff) {
        int index = foodstuffs.indexOf(foodstuff);
        foodstuffs.remove(foodstuff);
        for (Observer observer : getObservers()) {
            observer.notifyItemRemoved(index, this);
        }
    }

    public void clear() {
        foodstuffs.clear();
        for (Observer observer : getObservers()) {
            observer.notifyCleared(this);
        }
    }

    private void setTextViewText(View parent, int viewId, String text) {
        ((TextView) parent.findViewById(viewId)).setText(text);
    }

    private void setCalories(View foodstuffView, double calories) {
        String text = foodstuffView.getContext().getString(R.string.n_calories, toDecimalString(calories));
        setTextViewText(foodstuffView, R.id.extra_info_block, text);
    }

    public List<Foodstuff> getItems() {
        return new ArrayList<>(foodstuffs);
    }

    /**
     * Find the first occurrence of element with same id like new and replace it, if it is present
     * (optional operation)
     * @param newFoodstuff new element, contains id
     * @return true if adapter contained the element
     */
    public boolean replaceItem(Foodstuff newFoodstuff) {
        if (newFoodstuff.getId() == -1) {
            throw new IllegalArgumentException("Foodstuff has no id");
        }
        for (int index = 0; index < foodstuffs.size(); index++) {
            if (newFoodstuff.getId() == foodstuffs.get(index).getId()) {
                foodstuffs.set(index, newFoodstuff);
                for (Observer observer : getObservers()) {
                    observer.notifyItemChangedInChild(index, this);
                }
                return true;
            }
        }
        return false;
    }

    public boolean containsFoodstuffWithId(long id) {
        for (Foodstuff f : foodstuffs) {
            if (f.getId() == id) {
                return true;
            }
        }
        return false;
    }
}
