package korablique.recipecalculator.ui.usergoal;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import korablique.recipecalculator.R;

public class RobotoMonoArrayAdapter extends ArrayAdapter<String> {
    private static final int TEXT_SIZE_SP = 14;
    private Context context;
    private Typeface typeface;

    public RobotoMonoArrayAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.context = context;
        typeface = Typeface.createFromAsset(context.getAssets(), "roboto_mono_regular.ttf");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);
        v.setTypeface(typeface);
        v.setTextSize(TEXT_SIZE_SP);
        v.setTextColor(context.getResources().getColor(R.color.colorPrimaryText));
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) super.getDropDownView(position, convertView, parent);
        v.setTypeface(typeface);
        v.setTextSize(TEXT_SIZE_SP);
        v.setTextColor(context.getResources().getColor(R.color.colorPrimaryText));
        return v;
    }
}
