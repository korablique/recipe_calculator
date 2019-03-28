package korablique.recipecalculator.ui.chart;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;

public class DateValueFormatter implements IAxisValueFormatter {
    private long[] timestamps;

    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy");

    /**
     * @param timestamps values you want to show on axis
     */
    public DateValueFormatter(long[] timestamps) {
        this.timestamps = timestamps;
    }

    /**
     * MPAndroidChart Entry contains x and y values where it will be a point on chart.
     * But we want to show dates on one axis. So we pass timestamps array to constructor,
     * here we get an appropriate timestamp from array by index (axis value on chart)
     * and format it to human readable date
     * https://github.com/PhilJay/MPAndroidChart/wiki/The-AxisValueFormatter-interface
     * @param value the value to be formatted
     * @param axis  the axis the value belongs to
     * @return value that you want to show on axis (date)
     */
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return sdf.format(new DateTime(timestamps[(int) value]).toDate());
    }
}
