package korablique.recipecalculator.ui.profile;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;

import androidx.core.content.res.ResourcesCompat;
import korablique.recipecalculator.R;

public class ChartWrapper {
    private LineChart chart;
    private LineDataSet dataSet;

    public ChartWrapper(LineChart chart) {
        this.chart = chart;
        setUpChartUi();
    }

    private void setUpChartUi() {
        Resources resources = chart.getResources();
        int lineColor = resources.getColor(R.color.colorRed);
        int highLightColor = resources.getColor(R.color.colorYellow);
        int textColor = resources.getColor(R.color.colorPrimaryText);
        int secondaryTextColor = resources.getColor(R.color.colorSecondaryText);
        int gridColor = resources.getColor(R.color.colorBackground);
        int chartBackgroundColor = resources.getColor(android.R.color.white);
        float smallTextSize = 12f;
        Typeface robotoMonoMediumTypeface = ResourcesCompat.getFont(chart.getContext(), R.font.roboto_mono_medium);
        Typeface robotoMonoRegularTypeface = ResourcesCompat.getFont(chart.getContext(), R.font.roboto_mono_regular);

        // пока передаем null, т к сейчас только настраиваем UI
        dataSet = new LineDataSet(null, resources.getString(R.string.weight_change));
        dataSet.setColor(lineColor);
        dataSet.setCircleColor(lineColor);
        dataSet.setDrawCircleHole(false);
        dataSet.setCircleRadius(2f);
        dataSet.setValueTextColor(textColor);
        dataSet.setLineWidth(4f);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(highLightColor);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(textColor);
        xAxis.setTextSize(smallTextSize);
        xAxis.setTypeface(robotoMonoMediumTypeface);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        float dip = 0.77f;
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, resources.getDisplayMetrics());
        xAxis.setYOffset(px);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(textColor);
        yAxis.setTextSize(smallTextSize);
        yAxis.setTypeface(robotoMonoMediumTypeface);
        yAxis.setDrawAxisLine(false);
        yAxis.setGridColor(gridColor);
        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setTypeface(robotoMonoRegularTypeface);
        legend.setTextColor(secondaryTextColor);
        legend.setTextSize(smallTextSize);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.setBackgroundColor(chartBackgroundColor);
        chart.getDescription().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.invalidate(); // refresh
    }

    public void addData(List<Entry> entries) {
        for (Entry entry : entries) {
            dataSet.addEntry(entry);
        }
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }
}
