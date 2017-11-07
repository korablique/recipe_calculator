package korablique.recipecalculator.ui.history;

import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

public class ProgressViewHolder extends RecyclerView.ViewHolder {
    private LinearLayout nutritionProgress;

    public ProgressViewHolder(LinearLayout item) {
        super(item);
        nutritionProgress = item;
    }

    public LinearLayout getItem() {
        return nutritionProgress;
    }
}
