package korablique.recipecalculator.ui.chart;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;

public class DateValueFormatter implements IAxisValueFormatter {
    private long[] timestamps;

    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy");

    public DateValueFormatter(long[] timestamps) {
        this.timestamps = timestamps; }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return sdf.format(new DateTime(timestamps[(int) value]).toDate());
    }
}
