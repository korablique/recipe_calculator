package korablique.recipecalculator.ui.chart;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import korablique.recipecalculator.R;
import korablique.recipecalculator.ui.DecimalUtils;

public class HighlightOnTapMarker extends MarkerView {
    private TextView labelTextView;
    private MPPointF offset;

    /**
     * Constructor. Sets up the MarkerView with a custom layout resource.
     *
     * @param context
     * @param layoutResource the layout resource to use for the MarkerView
     */
    public HighlightOnTapMarker(Context context, int layoutResource) {
        super(context, layoutResource);
        labelTextView = findViewById(R.id.chart_highlighted_label_view);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        labelTextView.setText(DecimalUtils.toDecimalString(e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        if (offset == null) {
            // center the marker horizontally and vertically
            offset = new MPPointF(-(getWidth() / 2), -getHeight());
        }
        return offset;
    }
}
