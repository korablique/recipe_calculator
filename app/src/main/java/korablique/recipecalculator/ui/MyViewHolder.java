package korablique.recipecalculator.ui;

import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private View item;

    public MyViewHolder(View itemView) {
        super(itemView);
        item = itemView;
    }

    public View getItem() {
        return item;
    }
}
