package korablique.recipecalculator.ui.nestingadapters;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class AdapterParent extends RecyclerView.Adapter {
    @Nullable
    private RecyclerView recyclerView;
    private final List<AdapterChild> children = new ArrayList<>();
    private final ChildrenObserver childrenObserver = new ChildrenObserver();

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

        @Override
        public void notifyItemChangedInChild(int childIndex, AdapterChild child) {
            int parentIndex = transformChildPositionIntoParentPosition(childIndex, child);
            notifyItemChanged(parentIndex);
        }

        @Override
        public void notifyItemRemoved(int childIndex, AdapterChild child) {
            int parentIndex = transformChildPositionIntoParentPosition(childIndex, child);
            AdapterParent.this.notifyItemRemoved(parentIndex);
        }

        @Override
        public void notifyCleared(AdapterChild child) {
            AdapterParent.this.notifyDataSetChanged();
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

    public void addChild(AdapterChild child) {
        addChildToPosition(child, this.children.size());
    }

    public void addChildToPosition(AdapterChild child, int position) {
        this.children.add(position, child);
        child.addObserver(childrenObserver);
        notifyDataSetChanged();
        invalidateAllViewHolders();
    }

    private void invalidateAllViewHolders() {
        // Действия ниже вызовут onDetachedFromRecyclerView, поэтому
        // запомним наш recyclerView в локальную переменную.
        RecyclerView recyclerView = this.recyclerView;
        if (recyclerView != null) {
            // Удалим себя из recyclerView и заново добавим.
            //
            // Это костыль, нужный из-за того, что каждый child у нас имеет свой viewType,
            // и этот viewType вычисляется динамически исходя из позиции view - как только
            // добавляется новый child на позицию 0, он забирает себе viewType child'а, который до
            // этого был на позиции 0.
            //
            // Но RecyclerView запоминает, какие view holder'ы каким viewType'ам соответствуют,
            // отчего после создания view holder'а, он навсегда будет проассоциирован с viewType'ом,
            // и наша система смены "владельца" viewType'а ломается - recyclerView о ней не знает.
            //
            // Чтобы исправить эту проблему, заставляем recyclerView нас забыть, а потом снова
            // вставляем себя в него - recyclerView от этого забывает, какие view holder'ам
            // соответствуют какие viewType'ы.
            recyclerView.setAdapter(null);
            recyclerView.setAdapter(this);
        }
    }

    public void removeChild(AdapterChild child) {
        child.removeObserver(childrenObserver);
        this.children.remove(child);
        notifyDataSetChanged();
        invalidateAllViewHolders();
    }

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

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (this.recyclerView == recyclerView) {
            this.recyclerView = null;
        }
    }
}
