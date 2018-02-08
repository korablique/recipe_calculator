package korablique.recipecalculator.ui.mainscreen;


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
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_FOODSTUFF = 1;
    private List<Foodstuff> foodstuffs = new ArrayList<>();
    private int headerLayoutId;
    private Context context;

    public FoodstuffsAdapterChild(Context context, int headerLayoutId) {
        super(2);
        this.context = context;
        this.headerLayoutId = headerLayoutId;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            ViewGroup header = (ViewGroup) LayoutInflater.from(parent.getContext())
                    .inflate(headerLayoutId, parent, false);
            return new MyViewHolder(header);
        } else {
            ViewGroup foodstuffView = (ViewGroup) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.new_foodstuff_layout, parent, false);
            return new MyViewHolder(foodstuffView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition) {
        if (childPosition == 0) {
            // значит, это первый заголовок
            return;
        }
        int viewType = getItemViewType(childPosition);
        if (viewType == VIEW_TYPE_HEADER) {
            // заголовок
        } else {
            ViewGroup item = ((MyViewHolder) holder).getItem();
            Foodstuff foodstuff = foodstuffs.get(childPosition - 1);
            setTextViewText(item, R.id.new_foodstuff_name, foodstuff.getName());

            setCalories(item, foodstuff.getCalories());
        }
    }

    @Override
    public int getItemCount() {
        return foodstuffs.size() + 1;
    }

    @Override
    public int getItemViewType(int childPosition) {
        if (childPosition == 0) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_FOODSTUFF;
    }

    @Override
    public void addItems(List<Foodstuff> items) {
        int allFoodstuffsSizeBefore = foodstuffs.size();
        foodstuffs.addAll(items);
        // index = 1, потому что 0 - это заголовок
        for (int index = 1; index <= items.size(); index++) {
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
}
