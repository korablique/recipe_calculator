package korablique.recipecalculator.ui.bucketlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.DishNutritionCalculator;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.database.CreateRecipeResult;
import korablique.recipecalculator.database.RecipesRepository;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Recipe;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.NutritionValuesWrapper;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;
import korablique.recipecalculator.util.FloatUtils;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class BucketListActivity extends BaseActivity implements HasSupportFragmentInjector {
    public static final String EXTRA_CREATED_RECIPE = "EXTRA_CREATED_RECIPE";
    private static final String DISPLAYED_IN_CARD_FOODSTUFF_POSITION = "DISPLAYED_IN_CARD_FOODSTUFF_POSITION";
    @StringRes
    private static final int CARD_BUTTON_TEXT_RES = R.string.save;
    private PluralProgressBar pluralProgressBar;
    private NutritionValuesWrapper nutritionValuesWrapper;
    @Inject
    RecipesRepository recipesRepository;
    private BucketListAdapter adapter;
    private EditText totalWeightEditText;
    private Button saveAsRecipeButton;
    @Inject
    BucketList bucketList;
    private int displayedInCardFoodstuffPosition;
    private Card.OnMainButtonSimpleClickListener onAddFoodstuffButtonClickListener;
    @Inject
    TimeProvider timeProvider;

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }

    @Override
    protected Integer getLayoutId() {
        return R.layout.activity_bucket_list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup nutritionLayout = findViewById(R.id.nutrition_progress_with_values);
        pluralProgressBar = findViewById(R.id.new_nutrition_progress_bar);
        nutritionValuesWrapper = new NutritionValuesWrapper(this, nutritionLayout);

        List<Ingredient> ingredients = bucketList.getList();
        double totalWeight = countTotalWeight(ingredients);
        updateNutritionWrappers(ingredients, totalWeight);

        saveAsRecipeButton = findViewById(R.id.save_as_recipe_button);

        totalWeightEditText = findViewById(R.id.total_weight_edit_text);
        totalWeightEditText.setText(toDecimalString(totalWeight));
        totalWeightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                updateSaveButtonsEnability();
            }
        });

        BucketListAdapter.OnItemsCountChangeListener onItemsCountChangeListener = count -> {
            updateSaveButtonsEnability();
        };

        onAddFoodstuffButtonClickListener = new Card.OnMainButtonSimpleClickListener() {
            @Override
            public void onClick(WeightedFoodstuff newFoodstuff) {
                Ingredient oldIngredient = adapter.getItem(displayedInCardFoodstuffPosition);
                Ingredient newIngredient = Ingredient.create(newFoodstuff, oldIngredient.getComment());
                adapter.replaceItem(newIngredient, displayedInCardFoodstuffPosition);
                CardDialog.hideCard(BucketListActivity.this);

                bucketList.remove(oldIngredient);
                bucketList.add(newIngredient);

                double newTotalWeight = countTotalWeight(adapter.getItems());
                totalWeightEditText.setText(toDecimalString(newTotalWeight));

                updateNutritionWrappers(adapter.getItems(), newTotalWeight);
            }
        };

        CardDialog existingCardDialog = CardDialog.findCard(this);
        if (existingCardDialog != null) {
            existingCardDialog.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
        }

        BucketListAdapter.OnItemClickedObserver onItemClickedObserver = (ingredient, position) -> {
            displayedInCardFoodstuffPosition = position;
            CardDialog cardDialog = CardDialog.showCard(
                    BucketListActivity.this, ingredient.toWeightedFoodstuff());
            cardDialog.prohibitEditing(true);
            // чтобы не запутать пользователя. для удаления продукта из выбранных нужно его смахнуть
            cardDialog.prohibitDeleting(true);
            cardDialog.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
        };

        adapter = new BucketListAdapter(this, R.layout.new_foodstuff_layout, onItemsCountChangeListener, onItemClickedObserver);
        adapter.addItems(ingredients);

        RecyclerView ingredientsListRecyclerView = findViewById(R.id.ingredients_list);
        ingredientsListRecyclerView.setAdapter(adapter);

        OnSwipeItemCallback onSwipeItemCallback = new OnSwipeItemCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Ingredient deleting = adapter.getItem(position);
                adapter.deleteItem(position);
                bucketList.remove(deleting);
                double newWeight = countTotalWeight(adapter.getItems());
                totalWeightEditText.setText(toDecimalString(newWeight));
                updateNutritionWrappers(adapter.getItems(), newWeight);

                Snackbar snackbar = Snackbar.make(ingredientsListRecyclerView,
                        R.string.foodstuff_deleted, Snackbar.LENGTH_SHORT);
                snackbar.setAction(R.string.undo, v -> {
                    bucketList.add(deleting);
                    adapter.addItem(deleting, position);
                    totalWeightEditText.setText(toDecimalString(newWeight + deleting.getWeight()));
                    updateNutritionWrappers(adapter.getItems(), newWeight + deleting.getWeight());
                });
                snackbar.show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(onSwipeItemCallback);
        itemTouchHelper.attachToRecyclerView(ingredientsListRecyclerView);

        // диалог, появляющийся при сохранении блюда
        SaveRecipeDialog.OnSaveDishButtonClickListener saveDishButtonClickListener = (recipe) -> {
            Disposable d = recipesRepository.saveRecipeRx(recipe).subscribe((result) -> {
                if (result instanceof CreateRecipeResult.Ok) {
                    Recipe savedToDbRecipe = ((CreateRecipeResult.Ok) result).getRecipe();
                    SaveRecipeDialog dialog = SaveRecipeDialog.findDialog(BucketListActivity.this);
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    Toast.makeText(BucketListActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();

                    BucketListActivity.this.setResult(
                            Activity.RESULT_OK, createFoodstuffResultIntent(savedToDbRecipe));
                    bucketList.clear();
                    BucketListActivity.this.finish();
                } else if (result instanceof CreateRecipeResult.FoodstuffDuplicationError) {
                    Toast.makeText(BucketListActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show();
                } else {
                    throw new Error("Unhandled sealed class");
                }
            });
        };

        SaveRecipeDialog existingDialog = SaveRecipeDialog.findDialog(this);
        if (existingDialog != null) {
            existingDialog.setOnSaveRecipeButtonClickListener(saveDishButtonClickListener);
        }

        saveAsRecipeButton.setOnClickListener((view) -> {
            // т.к. вес готового продукта мог быть изменён, получаем его ещё раз
            float resultWeight = Float.parseFloat(totalWeightEditText.getText().toString());
            SaveRecipeDialog dialog = SaveRecipeDialog.showDialog(
                    BucketListActivity.this, ingredients, resultWeight, bucketList.getComment());
            dialog.setOnSaveRecipeButtonClickListener(saveDishButtonClickListener);
        });

        View cancelView = findViewById(R.id.button_close);
        cancelView.setOnClickListener(view -> BucketListActivity.this.finish());
    }

    public static Intent createFoodstuffResultIntent(Recipe recipe) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_CREATED_RECIPE, recipe);
        return resultIntent;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(DISPLAYED_IN_CARD_FOODSTUFF_POSITION, displayedInCardFoodstuffPosition);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        displayedInCardFoodstuffPosition = savedInstanceState.getInt(DISPLAYED_IN_CARD_FOODSTUFF_POSITION);
        CardDialog cardDialog = CardDialog.findCard(this);
        if (cardDialog != null) {
            cardDialog.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
        }
    }

    private void updateNutritionWrappers(List<Ingredient> ingredients, double totalWeight) {
        Nutrition totalNutrition = countTotalNutrition(ingredients);
        nutritionValuesWrapper.setNutrition(totalNutrition);
        // чтобы высчитать БЖУ на 100% для PluralProgressBar'а
        Nutrition nutritionPer100Percent =
                DishNutritionCalculator.calculateIngredients(ingredients, totalWeight);
        pluralProgressBar.setProgress(
                (float) nutritionPer100Percent.getProtein(),
                (float) nutritionPer100Percent.getFats(),
                (float) nutritionPer100Percent.getCarbs());
    }

    private double countTotalWeight(List<Ingredient> ingredients) {
        double result = 0;
        for (Ingredient foodstuff : ingredients) {
            result += foodstuff.getWeight();
        }
        return result;
    }

    private Nutrition countTotalNutrition(List<Ingredient> ingredients) {
        Nutrition totalNutrition = Nutrition.zero();
        for (Ingredient ingredient : ingredients) {
            totalNutrition = totalNutrition.plus(Nutrition.of100gramsOf(ingredient.getFoodstuff()));
        }
        return totalNutrition;
    }

    private void updateSaveButtonsEnability() {
        String text = totalWeightEditText.getText().toString();
        if (text.isEmpty()
                || FloatUtils.areFloatsEquals(Double.parseDouble(text), 0.0)
                || adapter.getItemCount() == 0) {
            saveAsRecipeButton.setEnabled(false);
        } else {
            saveAsRecipeButton.setEnabled(true);
        }
    }

    public static void start(
            Activity context,
            int requestCode) {
        context.startActivityForResult(createIntent(context), requestCode);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, BucketListActivity.class);
    }
}