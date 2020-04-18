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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.RecipesRepository;
import korablique.recipecalculator.model.Ingredient;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Recipe;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.NutritionValuesWrapper;
import korablique.recipecalculator.ui.bucketlist.model.ModifiedRecipeModel;
import korablique.recipecalculator.ui.bucketlist.model.ModifiedRecipeModelBucketList;
import korablique.recipecalculator.ui.bucketlist.model.ModifiedRecipeModelRecipeEditing;
import korablique.recipecalculator.ui.bucketlist.model.RecipeModelSaveChangesResult;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar;
import korablique.recipecalculator.util.FloatUtils;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class BucketListActivity extends BaseActivity implements HasSupportFragmentInjector {
    public static final String EXTRA_PRODUCED_RECIPE = "EXTRA_CREATED_RECIPE";
    private static final String DISPLAYED_IN_CARD_FOODSTUFF_POSITION = "DISPLAYED_IN_CARD_FOODSTUFF_POSITION";
    @VisibleForTesting
    public static final String ACTION_EDIT_RECIPE = "ACTION_EDIT_RECIPE";
    @VisibleForTesting
    public static final String EXTRA_RECIPE = "EXTRA_RECIPE";
    @StringRes
    private static final int CARD_BUTTON_TEXT_RES = R.string.save;
    private PluralProgressBar pluralProgressBar;
    private NutritionValuesWrapper nutritionValuesWrapper;
    @Inject
    RecipesRepository recipesRepository;
    private BucketListAdapter adapter;
    private EditText totalWeightEditText;
    private EditText recipeNameEditText;
    private Button saveAsRecipeButton;
    @Inject
    BucketList bucketList;
    @Inject
    MainThreadExecutor mainThreadExecutor;
    private int displayedInCardFoodstuffPosition;
    private Card.OnMainButtonSimpleClickListener onSaveFoodstuffButtonClickListener;
    @Inject
    TimeProvider timeProvider;

    private ModifiedRecipeModel recipeModel;

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

        TextView title = findViewById(R.id.title_text);
        if (ACTION_EDIT_RECIPE.equals(getIntent().getAction())) {
            Recipe recipe = getIntent().getParcelableExtra(EXTRA_RECIPE);
            recipeModel = new ModifiedRecipeModelRecipeEditing(
                    recipe, recipesRepository, mainThreadExecutor);
            title.setText(R.string.bucket_list_title_recipe);
        } else {
            recipeModel = new ModifiedRecipeModelBucketList(
                    bucketList, recipesRepository, mainThreadExecutor);
            title.setText(R.string.bucket_list_title_recipe_creation);
        }

        ViewGroup nutritionLayout = findViewById(R.id.nutrition_progress_with_values);
        pluralProgressBar = findViewById(R.id.new_nutrition_progress_bar);
        nutritionValuesWrapper = new NutritionValuesWrapper(this, nutritionLayout);

        saveAsRecipeButton = findViewById(R.id.save_as_recipe_button);
        recipeNameEditText = findViewById(R.id.recipe_name_edit_text);
        totalWeightEditText = findViewById(R.id.total_weight_edit_text);

        onSaveFoodstuffButtonClickListener = new Card.OnMainButtonSimpleClickListener() {
            @Override
            public void onClick(WeightedFoodstuff newFoodstuff) {
                Ingredient oldIngredient = adapter.getItem(displayedInCardFoodstuffPosition);
                Ingredient newIngredient = Ingredient.create(newFoodstuff, oldIngredient.getComment());
                adapter.replaceItem(newIngredient, displayedInCardFoodstuffPosition);
                float newTotalWeight = countTotalWeight(adapter.getItems());
                totalWeightEditText.setText(toDecimalString(newTotalWeight));

                CardDialog.hideCard(BucketListActivity.this);

                recipeModel.removeIngredient(oldIngredient);
                recipeModel.addIngredient(newIngredient);
                recipeModel.setTotalWeight(newTotalWeight);

                updateNutritionWrappers();
            }
        };
        CardDialog existingCardDialog = CardDialog.findCard(this);
        if (existingCardDialog != null) {
            existingCardDialog.setUpButton1(onSaveFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
        }

        BucketListAdapter.OnItemsCountChangeListener onItemsCountChangeListener = count -> {
            updateSaveButtonsEnability();
        };
        BucketListAdapter.OnItemClickedObserver onItemClickedObserver = (ingredient, position) -> {
            displayedInCardFoodstuffPosition = position;
            CardDialog cardDialog = CardDialog.showCard(
                    BucketListActivity.this, ingredient.toWeightedFoodstuff());
            cardDialog.setUpButton1(onSaveFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
        };
        BucketListAdapter.OnItemLongClickedObserver onItemLongClickedObserver = (ingredient, position, view) -> {
            PopupMenu menu = new PopupMenu(this, view);
            menu.inflate(R.menu.bucket_list_menu);
            menu.show();
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.delete_ingredient) {
                    adapter.deleteItem(position);
                    recipeModel.removeIngredient(ingredient);
                    float newWeight = countTotalWeight(adapter.getItems());
                    totalWeightEditText.setText(toDecimalString(newWeight));
                    recipeModel.setTotalWeight(newWeight);
                    updateNutritionWrappers();
                    return true;
                }
                return false;
            });
            return true;
        };
        adapter = new BucketListAdapter(
                this,
                R.layout.new_foodstuff_layout,
                onItemsCountChangeListener,
                onItemClickedObserver,
                onItemLongClickedObserver);
        adapter.addItems(recipeModel.getIngredients());
        RecyclerView ingredientsListRecyclerView = findViewById(R.id.ingredients_list);
        ingredientsListRecyclerView.setAdapter(adapter);

        saveAsRecipeButton.setOnClickListener((view) -> {
            recipeModel.flushChanges(result -> {
                if (result instanceof RecipeModelSaveChangesResult.Ok) {
                    Recipe savedToDbRecipe = ((RecipeModelSaveChangesResult.Ok) result).getRecipe();
                    Toast.makeText(BucketListActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                    BucketListActivity.this.setResult(
                            Activity.RESULT_OK, createRecipeResultIntent(savedToDbRecipe));
                    BucketListActivity.this.finish();
                } else if (result instanceof RecipeModelSaveChangesResult.FoodstuffDuplicationError) {
                    Toast.makeText(BucketListActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show();
                } else if (result instanceof RecipeModelSaveChangesResult.InternalError) {
                    Toast.makeText(BucketListActivity.this, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                } else {
                    throw new Error("Unhandled sealed class");
                }
            });
        });
        recipeNameEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                recipeModel.setName(s.toString());
                updateSaveButtonsEnability();
            }
        });
        totalWeightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                float totalWeight = 0;
                if (!s.toString().isEmpty()) {
                    totalWeight = Float.parseFloat(s.toString());
                }
                recipeModel.setTotalWeight(totalWeight);
                updateSaveButtonsEnability();
                updateNutritionWrappers();
            }
        });
        recipeNameEditText.setText(recipeModel.getName());
        totalWeightEditText.setText(toDecimalString(recipeModel.getTotalWeight()));

        View cancelView = findViewById(R.id.button_close);
        cancelView.setOnClickListener(view -> BucketListActivity.this.finish());

        updateNutritionWrappers();
    }

    public static Intent createRecipeResultIntent(Recipe recipe) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_PRODUCED_RECIPE, recipe);
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
            cardDialog.setUpButton1(onSaveFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
        }
    }

    private void updateNutritionWrappers() {
        Nutrition nutrition = Nutrition.zero();
        if (!FloatUtils.areFloatsEquals(0f, recipeModel.getTotalWeight(), 0.0001f)) {
            Recipe recipe = recipeModel.extractEditedRecipe();
            nutrition = Nutrition.of100gramsOf(recipe.getFoodstuff());
        }
        nutritionValuesWrapper.setNutrition(nutrition);
        pluralProgressBar.setProgress(
                (float) nutrition.getProtein(),
                (float) nutrition.getFats(),
                (float) nutrition.getCarbs());
    }

    private float countTotalWeight(List<Ingredient> ingredients) {
        float result = 0;
        for (Ingredient foodstuff : ingredients) {
            result += foodstuff.getWeight();
        }
        return result;
    }

    private void updateSaveButtonsEnability() {
        String text = totalWeightEditText.getText().toString();
        String name = recipeNameEditText.getText().toString().trim();
        if (text.isEmpty()
                || name.isEmpty()
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

    public static void startForRecipe(
            Fragment fragment,
            int requestCode,
            Recipe recipe) {
        fragment.startActivityForResult(createIntent(fragment.requireContext(), recipe), requestCode);
    }

    public static Intent createIntent(Context context, Recipe recipe) {
        Intent intent = new Intent(context, BucketListActivity.class);
        intent.setAction(ACTION_EDIT_RECIPE);
        intent.putExtra(EXTRA_RECIPE, recipe);
        return intent;
    }
}
