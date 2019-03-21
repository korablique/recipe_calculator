package korablique.recipecalculator.ui.chart;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import korablique.recipecalculator.ui.DecimalUtils;

public class WeightValueFormatter implements IAxisValueFormatter {
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return DecimalUtils.toDecimalString(value);
    }
}