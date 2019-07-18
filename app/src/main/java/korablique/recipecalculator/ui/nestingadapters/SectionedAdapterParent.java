package korablique.recipecalculator.ui.nestingadapters;

import androidx.annotation.NonNull;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

public class SectionedAdapterParent extends AdapterParent implements FastScrollRecyclerView.SectionedAdapter {

    @NonNull
    @Override
    public String getSectionName(int position) {
        ChildWithPosition childWithPosition = transformParentPositionIntoChildPosition(position);
        if (childWithPosition.child instanceof FastScrollRecyclerView.SectionedAdapter) {
            return ((SectionedFoodstuffsAdapterChild) childWithPosition.child)
                    .getSectionName(childWithPosition.position);
        } else {
            return "";
        }
    }
}
