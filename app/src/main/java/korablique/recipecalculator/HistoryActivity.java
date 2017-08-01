package korablique.recipecalculator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryActivity extends MyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // inflate nutrition_progress_layout
        LinearLayout parent = (LinearLayout) findViewById(R.id.history_parent);
        LinearLayout nutritionProgressDemo = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.nutrition_progress_layout, parent, false);
        // hide weight from header
        nutritionProgressDemo.findViewById(R.id.header).findViewById(R.id.column_name_weight).setVisibility(View.GONE);
        parent.addView(nutritionProgressDemo);

        // set progress (demo)
        ProgressBar proteinProgress = (ProgressBar)(nutritionProgressDemo.findViewById(R.id.protein_progress).findViewById(R.id.progress_bar));
        proteinProgress.setMax(94);
        proteinProgress.setProgress(30);
        ((TextView) nutritionProgressDemo.findViewById(R.id.protein_progress).findViewById(R.id.progress_text_view)).setText("30/94");

        ProgressBar fatProgress = ((ProgressBar) nutritionProgressDemo.findViewById(R.id.fat_progress).findViewById(R.id.progress_bar));
        fatProgress.setMax(47);
        fatProgress.setProgress(30);
        ((TextView) nutritionProgressDemo.findViewById(R.id.fat_progress).findViewById(R.id.progress_text_view)).setText("30/47");

        ProgressBar carbsProgress = ((ProgressBar) nutritionProgressDemo.findViewById(R.id.carbs_progress).findViewById(R.id.progress_bar));
        carbsProgress.setMax(237);
        carbsProgress.setProgress(30);
        ((TextView) nutritionProgressDemo.findViewById(R.id.carbs_progress).findViewById(R.id.progress_text_view)).setText("30/237");

        ProgressBar caloriesProgress = ((ProgressBar) nutritionProgressDemo.findViewById(R.id.calories_progress).findViewById(R.id.progress_bar));
        caloriesProgress.setMax(1771);
        caloriesProgress.setProgress(900);
        ((TextView) nutritionProgressDemo.findViewById(R.id.calories_progress).findViewById(R.id.progress_text_view)).setText("900/1771");
    }
}
