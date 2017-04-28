package korablique.recipecalculator;

import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private LinearLayout item;

    public MyViewHolder(LinearLayout itemView) {
        super(itemView);
        item = itemView;
    }

    public LinearLayout getItem() {
        return item;
    }
}
