package korablique.recipecalculator.ui.nestingadapters;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


public class AdapterParent extends RecyclerView.Adapter {
    private List<AdapterChild> children = new ArrayList<>();

    @VisibleForTesting
    static class ChildWithPosition {
        AdapterChild child;
        int position; // позиция относительно дочернего адаптера
    }

    /**
     * Этот внутренний нестатический класс создан вместо реализации AdapterParent'ом
     * интерфейса AdapterChild.Observer.
     * Т.о. методы интерфейса AdapterChild.Observer станут недоступны для (случайного) вызова
     * через ссылку на AdapterParent, и мы явно отделим публичный интерфейс AdapterParent'а
     * от деталей его реализации (то, что он обзёрвит AdapterChild - деталь реализации).
     */
    private class ChildrenObserver implements AdapterChild.Observer {
        @Override
        public void notifyItemInsertedToChild(int childIndex, AdapterChild child) {
            int parentIndex = transformChildPositionIntoParentPosition(childIndex, child);
            notifyItemInserted(parentIndex);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // надо найти соответствующего чайлда
        // трансформировать viewType в его viewType
        // вызвать OnCreateViewHolder с соответствующим viewType'ом у чайлда
        int accumulator = 0;
        for (AdapterChild child : children) {
            accumulator += child.getItemViewTypesCount();
            if (accumulator > viewType) {
                int childsViewType = viewType - (accumulator - child.getItemViewTypesCount());
                return child.onCreateViewHolder(parent, childsViewType);
            }
        }
        throw new IllegalStateException("ViewType not found");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChildWithPosition childWithPosition = transformParentPositionIntoChildPosition(position);
        childWithPosition.child.onBindViewHolder(holder, childWithPosition.position);
    }

    @Override
    public int getItemViewType(int position) {
        ChildWithPosition childWithPosition = transformParentPositionIntoChildPosition(position);
        int accumulator = 0;
        for (AdapterChild child : children) {
            if (child == childWithPosition.child) {
                return accumulator + child.getItemViewType(childWithPosition.position);
            }
            accumulator += child.getItemViewTypesCount();
        }
        throw new IllegalStateException("Child по данной позиции не найден");
    }

    @Override
    public int getItemCount() {
        int itemCount = 0;
        for (AdapterChild child : children) {
            itemCount += child.getItemCount();
        }
        return itemCount;
    }

    public void addChild(AdapterChild... children) {
        for (AdapterChild child : children) {
            this.children.add(child);
            child.addObserver(new ChildrenObserver());
        }
        notifyDataSetChanged();
    }

    @VisibleForTesting
    ChildWithPosition transformParentPositionIntoChildPosition(int parentPosition) {
        ChildWithPosition childWithPosition = new ChildWithPosition();
        int accumulator = 0; // суммирует размеры child'ов
        for (AdapterChild child : children) {
            accumulator += child.getItemCount();
            if (accumulator > parentPosition) {
                childWithPosition.child = child;
                childWithPosition.position = parentPosition - (accumulator - child.getItemCount());
                break;
            }
        }
        return childWithPosition;
    }

    @VisibleForTesting
    int transformChildPositionIntoParentPosition(int childPosition, AdapterChild child) {
        int accumulator = 0;
        int parentPosition = 0;
        for (AdapterChild ch : children) {
            if (ch == child) {
                parentPosition = accumulator + childPosition;
                break;
            } else {
                accumulator += ch.getItemCount();
            }
        }
        return parentPosition;
    }
}
