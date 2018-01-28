package korablique.recipecalculator.ui.mainscreen;


import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public interface AdapterChild {
    RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);
    void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition);
    int getItemCount();
    int getItemViewType(int childPosition);
    int getItemViewTypesCount();
    void addObserver(AdapterChildObserver observer);
    void removeObserver(AdapterChildObserver observer);
}
