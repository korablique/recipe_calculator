package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.ui.MyViewHolder;


public class BucketListAdapter extends RecyclerView.Adapter<MyViewHolder> {
    private final int VIEW_TYPE_INGREDIENT = 0;
    private final int VIEW_TYPE_ADD_INGREDIENT_BUTTON = 1;
    public interface OnItemClickedObserver {
        void onItemClicked(Ingredient ingredient, int position);
    }
    public interface OnItemLongClickedObserver {
        boolean onItemLongClicked(Ingredient ingredient, int position, View view);
    }
    private List<Ingredient> ingredients = new ArrayList<>();
    private Context context;
    private OnItemClickedObserver onItemClickedObserver = (ingredient, position) -> {};
    private OnItemLongClickedObserver onItemLongClickedObserver = (ingredient, position, view) -> false;
    @Nullable
    private Runnable onAddIngredientButtonObserver = null;

    public BucketListAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickedObserver(@Nullable OnItemClickedObserver observer) {
        if (observer == null) {
            observer = (ingredient, position) -> {};
        }
        onItemClickedObserver = observer;
    }

    public void setOnItemLongClickedObserver(@Nullable OnItemLongClickedObserver observer) {
        if (observer == null) {
            observer = (ingredient, position, view) -> false;
        }
        onItemLongClickedObserver = observer;
    }

    public void setUpAddIngredientButton(@Nullable Runnable clickObserver) {
        boolean wasSet = onAddIngredientButtonObserver != null;
        onAddIngredientButtonObserver = clickObserver;
        boolean isSet = onAddIngredientButtonObserver != null;
        if (!wasSet && isSet) {
            notifyItemInserted(ingredients.size());
        } else if (wasSet && !isSet) {
            notifyItemRemoved(ingredients.size());
        }
    }

    public void deinitAddIngredientButton() {
        setUpAddIngredientButton(null);
    }

    @Override
    public int getItemViewType(int position) {
        if (onAddIngredientButtonObserver != null && position == ingredients.size()) {
            return VIEW_TYPE_ADD_INGREDIENT_BUTTON;
        } else {
            return VIEW_TYPE_INGREDIENT;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_INGREDIENT) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.new_foodstuff_layout, parent, false));
        } else if (viewType == VIEW_TYPE_ADD_INGREDIENT_BUTTON) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bucket_list_add_ingredient_button, parent, false));
        } else {
            throw new Error("Not supported view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int displayedPosition) {
        if (onAddIngredientButtonObserver != null && displayedPosition == ingredients.size()) {
            holder.getItem().findViewById(R.id.bucket_list_add_ingredient_button).setOnClickListener((v) -> {
                onAddIngredientButtonObserver.run();
            });
            return;
        }
        View item = holder.getItem();
        final Ingredient ingredient = ingredients.get(displayedPosition);

        setTextViewText(item, R.id.name, ingredient.getFoodstuff().getName());
        setTextViewText(item, R.id.extra_info_block, context.getString(R.string.n_gramms, Math.round(ingredient.getWeight())));

        item.setOnClickListener(v -> {
            onItemClickedObserver.onItemClicked(ingredient, holder.getAdapterPosition());
        });
        item.setOnLongClickListener(v -> {
            return onItemLongClickedObserver.onItemLongClicked(
                    ingredient, holder.getAdapterPosition(), holder.itemView);
        });
    }

    @Override
    public int getItemCount() {
        if (onAddIngredientButtonObserver == null) {
            return ingredients.size();
        } else {
            return ingredients.size() + 1;
        }
    }

    public void setItems(List<Ingredient> newIngredients) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return ingredients.size();
            }
            @Override
            public int getNewListSize() {
                return newIngredients.size();
            }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // If 2 items have same foodstuff, they are most likely same items,
                // but with different weight. So, if 2 items have same foodstuffs, we consider
                // them to be same items.
                return ingredients.get(oldItemPosition).getFoodstuff().equals(
                        newIngredients.get(newItemPosition).getFoodstuff());
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return ingredients.get(oldItemPosition).equals(
                        newIngredients.get(newItemPosition));
            }
        });

        ingredients.clear();
        ingredients.addAll(newIngredients);
        diff.dispatchUpdatesTo(this);
    }

    private <T> void setTextViewText(View parent, int viewId, T text) {
        ((TextView) parent.findViewById(viewId)).setText(text.toString());
    }
}
