package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.MyViewHolder;


public class BucketListAdapter extends RecyclerView.Adapter<MyViewHolder> {
    public interface OnItemsCountChangeListener {
        void onItemsCountChange(int count);
    }
    public interface OnItemClickedObserver {
        void onItemClicked(WeightedFoodstuff foodstuff, int position);
    }
    private List<WeightedFoodstuff> allFoodstuffs = new ArrayList<>();
    private Context context;
    @LayoutRes
    private int itemLayoutRes;
    private OnItemsCountChangeListener listener;
    private OnItemClickedObserver onItemClickedObserver;

    public BucketListAdapter(
            Context context,
            @LayoutRes int itemLayoutId,
            OnItemsCountChangeListener listener,
            OnItemClickedObserver onItemClickedObserver) {
        this.context = context;
        this.itemLayoutRes = itemLayoutId;
        this.listener = listener;
        this.onItemClickedObserver = onItemClickedObserver;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int displayedPosition) {
        View item = holder.getItem();
        final WeightedFoodstuff foodstuff = getItem(displayedPosition);

        setTextViewText(item, R.id.name, foodstuff.getName());
        setTextViewText(item, R.id.extra_info_block, context.getString(R.string.n_gramms, Math.round(foodstuff.getWeight())));

        item.setOnClickListener(v -> {
            onItemClickedObserver.onItemClicked(foodstuff, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return allFoodstuffs.size();
    }

    public void addItems(List<WeightedFoodstuff> foodstuffs) {
        int allFoodstuffsSizeBefore = allFoodstuffs.size();
        allFoodstuffs.addAll(foodstuffs);
        for (int index = 0; index < foodstuffs.size(); index++) {
            notifyItemInserted(allFoodstuffsSizeBefore + index);
        }
        listener.onItemsCountChange(getItemCount());
    }

    public void addItem(WeightedFoodstuff foodstuff) {
        addItems(Collections.singletonList(foodstuff));
    }

    public void addItem(WeightedFoodstuff foodstuff, int position) {
        allFoodstuffs.add(position, foodstuff);
        notifyItemInserted(position);
        listener.onItemsCountChange(getItemCount());
    }

    public void deleteItem(int displayedPosition) {
        allFoodstuffs.remove(displayedPosition);
        notifyItemRemoved(displayedPosition);
        listener.onItemsCountChange(getItemCount());
    }

    public void replaceItem(WeightedFoodstuff newFoodstuff, int displayedPosition) {
        allFoodstuffs.set(displayedPosition, newFoodstuff);
        notifyItemChanged(displayedPosition);
    }

    public WeightedFoodstuff getItem(int displayedPosition) {
        return allFoodstuffs.get(displayedPosition);
    }

    public List<WeightedFoodstuff> getItems() {
        return Collections.unmodifiableList(allFoodstuffs);
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }
}
