package korablique.recipecalculator.ui;

import android.content.Context;

import java.util.List;

import korablique.recipecalculator.ui.userparameters.RobotoMonoArrayAdapter;

public class ArrayAdapterWithDisabledItem extends RobotoMonoArrayAdapter {
    private int disableItemIndex;

    public ArrayAdapterWithDisabledItem(Context context, int textViewResourceId, List<String> objects, int disableItemIndex) {
        super(context, textViewResourceId, objects);
        this.disableItemIndex = disableItemIndex;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != disableItemIndex;
    }
}
