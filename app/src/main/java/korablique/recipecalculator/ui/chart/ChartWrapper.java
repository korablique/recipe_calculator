package korablique.recipecalculator.ui.chart;

import android.content.res.Resources;
import android.graphics.Typeface;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.res.ResourcesCompat;
import korablique.recipecalculator.R;
import korablique.recipecalculator.model.UserParameters;

public class ChartWrapper {
    private LineChart chart;
    private LineData lineData;

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
        xAxis.setEnabled(false);
        xAxis.setDrawGridLines(false);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setValueFormatter(new WeightValueFormatter());
        yAxis.setTextColor(textColor);
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

        lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.setBackgroundColor(chartBackgroundColor);
        chart.getDescription().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.invalidate(); // refresh
    }

    public void setData(List<UserParameters> userParametersList) {
        List<Entry> chartEntries = new ArrayList<>();
        for (int index = 0; index < userParametersList.size(); index++) {
            // здесь задаются координаты точек на графике,
            // отображение значений по осям задаётся в Formatter'ах
            float x = index;
            float y = userParametersList.get(index).getWeight();
            chartEntries.add(new Entry(x, y));
        }
        dataSet.clear();
        for (Entry entry : chartEntries) {
            lineData.addEntry(entry, lineData.getDataSetCount() - 1);
        }
        chart.notifyDataSetChanged();
        chart.invalidate();
    }
}
