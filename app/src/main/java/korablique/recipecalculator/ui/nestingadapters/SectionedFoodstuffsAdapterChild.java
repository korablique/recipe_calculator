package korablique.recipecalculator.ui.nestingadapters;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.jetbrains.annotations.NotNull;

public class SectionedFoodstuffsAdapterChild extends FoodstuffsAdapterChild implements FastScrollRecyclerView.SectionedAdapter {
    public SectionedFoodstuffsAdapterChild(ClickObserver clickObserver) {
        super(clickObserver);
    }

    @NotNull
    @Override
    public String getSectionName(int position) {
        return foodstuffs.get(position).getName().substring(0, 1);
    }
}
