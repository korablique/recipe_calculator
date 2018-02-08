package korablique.recipecalculator.ui.mainscreen;


import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.model.Foodstuff;

public abstract class AdapterChild {
    private List<Observer> observers = new ArrayList<>();
    private int itemViewTypesCount;

    public interface Observer {
        void notifyItemInsertedToChild(int index, AdapterChild child);
    }

    public AdapterChild(int itemViewTypesCount) {
        this.itemViewTypesCount = itemViewTypesCount;
    }

    public abstract RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);
    public abstract void onBindViewHolder(RecyclerView.ViewHolder holder, int childPosition);
    public abstract int getItemCount();
    public abstract int getItemViewType(int childPosition);

    public int getItemViewTypesCount() {
        return itemViewTypesCount;
    }

    public abstract void addItems(List<Foodstuff> items);

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    protected List<Observer> getObservers() {
        return observers;
    }
}
