package korablique.recipecalculator.ui;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

public class ArrayAdapterWithDisabledItem extends ArrayAdapter<String> {
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
