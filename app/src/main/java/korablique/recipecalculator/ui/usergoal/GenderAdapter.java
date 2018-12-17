package korablique.recipecalculator.ui.usergoal;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

public class GenderAdapter extends ArrayAdapter<String> {
    private int disableItemIndex;

    public GenderAdapter(Context context, int textViewResourceId, List<String> objects, int disableItemIndex) {
        super(context, textViewResourceId, objects);
        this.disableItemIndex = disableItemIndex;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != disableItemIndex;
    }
}
