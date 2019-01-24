package korablique.recipecalculator.ui;

import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private ViewGroup item;

    public MyViewHolder(ViewGroup itemView) {
        super(itemView);
        item = itemView;
    }

    public ViewGroup getItem() {
        return item;
    }
}
