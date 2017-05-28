package korablique.recipecalculator;

import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

public class FoodstuffViewHolder extends RecyclerView.ViewHolder {
    private LinearLayout item;

    public FoodstuffViewHolder(LinearLayout itemView) {
        super(itemView);
        item = itemView;
    }

    public LinearLayout getItem() {
        return item;
    }
}
