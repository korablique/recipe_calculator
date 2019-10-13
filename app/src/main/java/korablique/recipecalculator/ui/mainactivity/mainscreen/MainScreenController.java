package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.FoodstuffsTopList;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage;
import korablique.recipecalculator.ui.nestingadapters.FoodstuffsAdapterChild;
import korablique.recipecalculator.ui.nestingadapters.SectionedAdapterParent;
import korablique.recipecalculator.ui.nestingadapters.SectionedFoodstuffsAdapterChild;
import korablique.recipecalculator.ui.nestingadapters.SingleItemAdapterChild;

import static korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragment.SEARCH_RESULTS_FRAGMENT_TAG;

@FragmentScope
public class MainScreenController
        implements FragmentCallbacks.Observer,
        ActivityCallbacks.Observer {
    private static final String EXTRA_INITIAL_TOP = "EXTRA_INITIAL_TOP";
    private static final String EXTRA_ALL_FOODSTUFFS_FIRST_BATCH = "EXTRA_ALL_FOODSTUFFS_FIRST_BATCH";
    private final BaseActivity context;
    private final BaseFragment fragment;
    private final ActivityCallbacks activityCallbacks;
    private final FoodstuffsList foodstuffsList;
    private final FoodstuffsTopList topList;
    private final MainActivitySelectedDateStorage selectedDateStorage;
    private final MainScreenCardController cardController;
    private final MainScreenReadinessDispatcher readinessDispatcher;
    private SectionedAdapterParent adapterParent;
    private FoodstuffsAdapterChild topAdapterChild;
    private SectionedFoodstuffsAdapterChild foodstuffAdapterChild;
    private SelectedFoodstuffsSnackbar snackbar;

    private BucketList.Observer bucketListObserver = new BucketList.Observer() {
        @Override
        public void onFoodstuffAdded(WeightedFoodstuff weightedFoodstuff) {
            snackbar.addFoodstuff(weightedFoodstuff);
            snackbar.show();
        }
        @Override
        public void onFoodstuffRemoved(WeightedFoodstuff weightedFoodstuff) {
            snackbar.update(BucketList.getInstance().getList());
        }
    };
    private FoodstuffsTopList.Observer topListObserver = new FoodstuffsTopList.Observer() {
        @Override
        public void onFoodstuffsTopPossiblyChanged() {
            topList.getTopList(foodstuffs -> {
                if (topAdapterChild != null) {
                    topAdapterChild.clear();
                }
                fillTop(foodstuffs);
            });
        }
    };
    private boolean isTopFilledFromArguments;
    private boolean isAllFoodstuffsListFilledFromArguments;

    @Inject
    public MainScreenController(
            BaseActivity context,
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            ActivityCallbacks activityCallbacks,
            FoodstuffsTopList topList,
            FoodstuffsList foodstuffsList,
            MainActivitySelectedDateStorage selectedDateStorage,
            MainScreenCardController cardController,
            MainScreenReadinessDispatcher readinessDispatcher) {
        this.context = context;
        this.fragment = fragment;
        this.activityCallbacks = activityCallbacks;
        this.topList = topList;
        this.foodstuffsList = foodstuffsList;
        this.selectedDateStorage = selectedDateStorage;
        this.cardController = cardController;
        this.readinessDispatcher = readinessDispatcher;
        fragmentCallbacks.addObserver(this);
        activityCallbacks.addObserver(this);
    }

    public static Bundle createArguments(
            ArrayList<Foodstuff> top, ArrayList<Foodstuff> allFoodstuffsFirstBatch) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(EXTRA_INITIAL_TOP, top);
        bundle.putParcelableArrayList(EXTRA_ALL_FOODSTUFFS_FIRST_BATCH, allFoodstuffsFirstBatch);
        return bundle;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        // при нажатии назад вызывается onFragmentViewCreated, т к старый fragmentView удалился,
        // а поля остались проинициализированными, и адаптеры добавлены в recyclerView
        // старного fragmentView. мы их зануляем, чтоб они заново инициализировались
        adapterParent = null;
        topAdapterChild = null;
        foodstuffAdapterChild = null;

        snackbar = new SelectedFoodstuffsSnackbar(fragmentView);

        RecyclerView recyclerView = fragmentView.findViewById(R.id.main_screen_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        BucketList bucketList = BucketList.getInstance();
        bucketList.addObserver(bucketListObserver);
        snackbar.update(bucketList.getList());

        foodstuffsList.addObserver(new FoodstuffsList.Observer() {
            @Override
            public void onFoodstuffSaved(Foodstuff savedFoodstuff, int index) {
                foodstuffAdapterChild.addItem(savedFoodstuff, index);
            }

            @Override
            public void onFoodstuffEdited(Foodstuff edited) {
                if (topAdapterChild != null && topAdapterChild.containsFoodstuffWithId(edited.getId())) {
                    topAdapterChild.replaceItem(edited);
                }
                foodstuffAdapterChild.replaceItem(edited);
            }

            @Override
            public void onFoodstuffDeleted(Foodstuff deleted) {
                if (topAdapterChild != null && topAdapterChild.containsFoodstuffWithId(deleted.getId())) {
                    topAdapterChild.removeItem(deleted);
                }
                foodstuffAdapterChild.removeItem(deleted);
            }
        });

        snackbar.setOnBasketClickRunnable(() -> {
            BucketListActivity.start(
                    context,
                    RequestCodes.MAIN_SCREEN_BUCKET_LIST_CREATE_FOODSTUFF,
                    new ArrayList<>(snackbar.getSelectedFoodstuffs()),
                    selectedDateStorage.getSelectedDate());
        });
        snackbar.setOnDismissListener(() -> {
            List<WeightedFoodstuff> dismissedFoodstuffs = new ArrayList<>(snackbar.getSelectedFoodstuffs());
            bucketList.clear();

            Snackbar snackbar = Snackbar.make(fragmentView, R.string.foodstuffs_deleted, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.undo, v -> {
                bucketList.add(dismissedFoodstuffs);
            });
            snackbar.show();
        });

        adapterParent = new SectionedAdapterParent();
        recyclerView.setAdapter(adapterParent);

        fillListsFromArguments();

        topList.getTopList(foodstuffs -> {
            if (isTopFilledFromArguments && topAdapterChild != null) {
                topAdapterChild.clear();
            }
            fillTop(foodstuffs);
            isTopFilledFromArguments = false;

            foodstuffsList.getAllFoodstuffs(batch -> {
                if (isAllFoodstuffsListFilledFromArguments && foodstuffAdapterChild != null) {
                    foodstuffAdapterChild.clear();
                }
                fillAllFoodstuffsList(batch);
                isAllFoodstuffsListFilledFromArguments = false;
            }, unused -> {
                readinessDispatcher.onMainScreenReady();
            });
        });
        topList.addObserver(topListObserver);
    }

    private void fillListsFromArguments() {
        Bundle args = fragment.getArguments();
        if (args != null) {
            List<Foodstuff> top = args.getParcelableArrayList(EXTRA_INITIAL_TOP);
            if (top != null) {
                fillTop(top);
                isTopFilledFromArguments = true;
            }
            List<Foodstuff> allFoodstuffsFirstBatch = args.getParcelableArrayList(EXTRA_ALL_FOODSTUFFS_FIRST_BATCH);
            if (allFoodstuffsFirstBatch != null) {
                fillAllFoodstuffsList(allFoodstuffsFirstBatch);
                isAllFoodstuffsListFilledFromArguments = true;
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        BucketList bucketList = BucketList.getInstance();
        bucketList.removeObserver(bucketListObserver);
        topList.removeObserver(topListObserver);

        activityCallbacks.removeObserver(this);
    }

    private void fillAllFoodstuffsList(List<Foodstuff> batch) {
        if (foodstuffAdapterChild == null) {
            SingleItemAdapterChild.Observer observer = v -> {
                View addNewFoodstuffButton = v.findViewById(R.id.add_new_foodstuff);
                addNewFoodstuffButton.setOnClickListener(v1 -> {
                    EditFoodstuffActivity.startForCreation(
                            fragment, RequestCodes.MAIN_SCREEN_CREATE_FOODSTUFF);
                });
            };
            SingleItemAdapterChild foodstuffsTitle = new SingleItemAdapterChild(
                    R.layout.all_foodstuffs_header, observer);
            foodstuffAdapterChild = new SectionedFoodstuffsAdapterChild(
                    context, (foodstuff, pos) -> cardController.showCard(foodstuff));
            adapterParent.addChild(foodstuffsTitle);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(batch);
    }

    private void fillTop(List<Foodstuff> foodstuffs) {
        if (!foodstuffs.isEmpty()) {
            if (topAdapterChild == null) {
                topAdapterChild = new FoodstuffsAdapterChild(
                        context, (foodstuff, pos) -> cardController.showCard(foodstuff));
                SingleItemAdapterChild topTitle = new SingleItemAdapterChild(R.layout.top_foodstuffs_header);
                adapterParent.addChildToPosition(topTitle, 0);
                adapterParent.addChildToPosition(topAdapterChild, 1);
            }
            topAdapterChild.addItems(foodstuffs);
        }
    }

    @Override
    public void onFragmentSaveInstanceState(Bundle outState) {
        snackbar.onSaveInstanceState(outState);
    }

    @Override
    public void onFragmentRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            snackbar.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onFragmentResume() {
        BucketList bucketList = BucketList.getInstance();
        snackbar.update(bucketList.getList());
    }

    @Override
    public void onFragmentActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == RequestCodes.MAIN_SCREEN_CREATE_FOODSTUFF) {
            Foodstuff foodstuff = data.getParcelableExtra(EditFoodstuffActivity.EXTRA_RESULT_FOODSTUFF);
            cardController.showCard(foodstuff);
        } else if (requestCode == RequestCodes.MAIN_SCREEN_BUCKET_LIST_CREATE_FOODSTUFF) {
            Foodstuff foodstuff = data.getParcelableExtra(BucketListActivity.EXTRA_CREATED_FOODSTUFF);
            cardController.showCard(foodstuff);
        }
    }
}
