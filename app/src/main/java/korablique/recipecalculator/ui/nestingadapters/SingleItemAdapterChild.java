package korablique.recipecalculator.ui.nestingadapters;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import korablique.recipecalculator.ui.MyViewHolder;

public class SingleItemAdapterChild extends AdapterChild {
    @LayoutRes
    private final int layoutId;

    public SingleItemAdapterChild(@LayoutRes int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewGroup view = (ViewGroup) inflater.inflate(layoutId, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition) {
        // У нас всегда 1 чайлд
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int childPosition) {
        return 0;
    }
}
