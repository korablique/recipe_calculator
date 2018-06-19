package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWithValuesWrapper;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.nestingadapters.AdapterParent;
import korablique.recipecalculator.ui.nestingadapters.FoodstuffsAdapterChild;

public class BucketListActivity extends BaseActivity {
    private static final String EXTRA_FOODSTUFFS_LIST = "EXTRA_FOODSTUFFS_LIST";
    private NutritionProgressWithValuesWrapper nutritionWrapper;
    @Inject
    DatabaseWorker databaseWorker;
    private double resultWeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bucket_list);

        nutritionWrapper =
                new NutritionProgressWithValuesWrapper(this, findViewById(R.id.nutrition_progress_with_values));

        List<Foodstuff> foodstuffs = getIntent().getParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST);
        if (foodstuffs == null) {
            throw new IllegalArgumentException("Can't start without " + EXTRA_FOODSTUFFS_LIST);
        }

        Nutrition totalNutrition = Nutrition.zero();
        double totalWeight = 0.0;
        for (Foodstuff foodstuff : foodstuffs) {
            totalNutrition = totalNutrition.plus(Nutrition.of(foodstuff));
            totalWeight += foodstuff.getWeight();
        }
        nutritionWrapper.setNutrition(totalNutrition);
        TextView totalWeightTextView = findViewById(R.id.total_weight_text_view);
        totalWeightTextView.setText(getString(R.string.weight_is_n_gramms, totalWeight));
        resultWeight = totalWeight;

        AdapterParent adapterParent = new AdapterParent();
        FoodstuffsAdapterChild foodstuffsAdapter =
                new FoodstuffsAdapterChild(this, (foodstuff, displayedPosition) -> {});
        adapterParent.addChild(foodstuffsAdapter);
        foodstuffsAdapter.addItems(foodstuffs);

        RecyclerView foodstuffsListRecyclerView = findViewById(R.id.foodstuffs_list);
        foodstuffsListRecyclerView.setAdapter(adapterParent);

        SaveDishDialog.OnSaveDishButtonClickListener saveDishButtonClickListener = (foodstuff) -> {
            databaseWorker.saveFoodstuff(BucketListActivity.this, foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
                @Override
                public void onResult(long id) {
                    SaveDishDialog dialog = SaveDishDialog.findDialog(BucketListActivity.this);
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    Toast.makeText(BucketListActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onDuplication() {
                    Toast.makeText(BucketListActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show();
                }
            });
        };

        SaveDishDialog existingDialog = SaveDishDialog.findDialog(this);
        if (existingDialog != null) {
            existingDialog.setOnSaveDishButtonClickListener(saveDishButtonClickListener);
        }

        findViewById(R.id.save_as_single_foodstuff_button).setOnClickListener((view) -> {
            SaveDishDialog dialog = SaveDishDialog.showDialog(this, foodstuffs, resultWeight);
            dialog.setOnSaveDishButtonClickListener(saveDishButtonClickListener);
        });

        findViewById(R.id.edit_bucket_button).setOnClickListener((view) -> {
            Toast.makeText(this, "Not supported yet", Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.save_to_history_button).setOnClickListener((view) -> {
            HistoryActivity.startAndAdd(foodstuffsAdapter.getItems(), this);
            finish();
        });
    }

    public static void start(ArrayList<Foodstuff> foodstuffs, Context context) {
        context.startActivity(createStartIntentFor(foodstuffs, context));
    }

    public static Intent createStartIntentFor(List<Foodstuff> foodstuffs, Context context) {
        Intent intent = new Intent(context, BucketListActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(foodstuffs));
        return intent;
    }
}
