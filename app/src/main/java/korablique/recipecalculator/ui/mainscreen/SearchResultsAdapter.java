package korablique.recipecalculator.ui.mainscreen;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.MyViewHolder;


public class SearchResultsAdapter extends RecyclerView.Adapter<MyViewHolder> {
    public interface OnItemClickedObserver {
        void onItemClicked(Foodstuff foodstuff, int position);
    }
    private List<Foodstuff> searchResults = new ArrayList<>();
    private OnItemClickedObserver onItemClickedObserver;

    public SearchResultsAdapter(OnItemClickedObserver observer) {
        this.onItemClickedObserver = observer;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LinearLayout item = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.new_foodstuff_layout, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ViewGroup item = holder.getItem();
        setTextViewText(item, R.id.name, searchResults.get(position).getName());
        setTextViewText(item, R.id.extra_info_block, searchResults.get(position).getCalories());
        item.setOnClickListener(v -> {
            onItemClickedObserver.onItemClicked(searchResults.get(position), holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void addItems(List<Foodstuff> foodstuffs) {
        int allFoodstuffsSizeBefore = searchResults.size();
        searchResults.addAll(foodstuffs);
        for (int index = 0; index < foodstuffs.size(); index++) {
            notifyItemInserted(allFoodstuffsSizeBefore + index);
        }
    }

    public void replaceItem(Foodstuff newFoodstuff, int displayedPosition) {
        searchResults.set(displayedPosition, newFoodstuff);
        notifyItemChanged(displayedPosition);
    }

    public Foodstuff getItem(int displayedPosition) {
        return searchResults.get(displayedPosition);
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }
}
