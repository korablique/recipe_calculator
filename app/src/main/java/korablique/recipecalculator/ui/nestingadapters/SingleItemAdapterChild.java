package korablique.recipecalculator.ui.nestingadapters;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import korablique.recipecalculator.ui.MyViewHolder;

public class SingleItemAdapterChild extends AdapterChild {
    public interface Observer {
        void onViewShown(View v);
    }
    @LayoutRes
    private final int layoutId;
    private Observer observer;

    public SingleItemAdapterChild(@LayoutRes int layoutId, Observer observer) {
        this.layoutId = layoutId;
        this.observer = observer;
    }

    public SingleItemAdapterChild(@LayoutRes int layoutId) {
        this(layoutId, null);
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

        ViewGroup item = ((MyViewHolder) holder).getItem();
        if (observer != null) {
            observer.onViewShown(item);
        }
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
