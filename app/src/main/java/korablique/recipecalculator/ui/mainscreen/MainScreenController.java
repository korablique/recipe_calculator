package korablique.recipecalculator.ui.mainscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.StringRes;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.TopList;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.card.NewCard;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.nestingadapters.AdapterParent;
import korablique.recipecalculator.ui.nestingadapters.FoodstuffsAdapterChild;
import korablique.recipecalculator.ui.nestingadapters.SingleItemAdapterChild;

import static android.app.Activity.RESULT_OK;
import static korablique.recipecalculator.IntentConstants.EDIT_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.EDIT_RESULT;
import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;

@FragmentScope
public class MainScreenController extends FragmentCallbacks.Observer {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    @StringRes
    private static final int CARD_BUTTON_TEXT_RES = R.string.add_foodstuff;
    private BaseActivity context;
    private BaseFragment fragment;
    private Lifecycle lifecycle;
    private final FoodstuffsList foodstuffsList;
    private final TopList topList;
    private AdapterParent adapterParent;
    private FoodstuffsAdapterChild topAdapterChild;
    private FoodstuffsAdapterChild foodstuffAdapterChild;
    private FloatingSearchView searchView;
    private SelectedFoodstuffsSnackbar snackbar;
    private NewCard.OnAddFoodstuffButtonClickListener cardDialogOnAddFoodstuffButtonClickListener;
    private NewCard.OnEditButtonClickListener cardDialogOnEditButtonClickListener;
    // Действие, которое нужно выполнить с диалогом после savedInstanceState (показ или скрытие диалога)
    // Поле нужно, чтобы приложение не крешило при показе диалога, когда тот показывается в момент,
    // когда активити в фоне (запаузена).
    // fragment manager не позваляет выполнять никакие операции с фрагментами, пока активити запаузена -
    // ведь fragment manager уже сохранил состояние всех фрагментов,
    // и ещё раз это сделать до резьюма активити невозможно (больше не вызовается Activity.onSaveInstanceState).
    // Чтобы сохранение стейта случилось ещё раз, активити должна выйти на передний план.
    // А когда активити в фоне, неизвестно, выйдет ли она на передний план - fm от этой неизвестности страхуется исключением.
    // (Если не выйдет, то будет потеря состояния.)
    // (Тут иерархичное подчинение - ОС требует от Активити сохранение стейта,
    // Активти требует от всех своих компонентов, в т.ч. от fm,
    // а fm требует сохранение стейта от всех своих компонентов, и т.д.)
    private Runnable dialogAction;

    // Disposable чтобы отменить последний начатый поиск при старте нового.
    // Такая отмена нужна на случай, если поиск 2 будет быстрее поиска 1 - чтобы между
    // поисками не было состояния гонки и именно последний поиск всегда был отображен.
    // По-умолчанию Disposables.empty() чтобы не нужно было делать проверки на null.
    private Disposable lastSearchDisposable = Disposables.empty();
    private BucketList.Observer bucketListObserver = new BucketList.Observer() {
        @Override
        public void onFoodstuffAdded(WeightedFoodstuff weightedFoodstuff) {
            snackbar.addFoodstuff(weightedFoodstuff);
            snackbar.show();
        }
        public void onFoodstuffRemoved(WeightedFoodstuff weightedFoodstuff) {
            snackbar.update(BucketList.getInstance().getList());
        }
    };

    @Inject
    MainScreenController(
            BaseActivity context,
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            Lifecycle lifecycle,
            TopList topList,
            FoodstuffsList foodstuffsList) {
        this.context = context;
        this.fragment = fragment;
        this.lifecycle = lifecycle;
        this.topList = topList;
        this.foodstuffsList = foodstuffsList;
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        snackbar = new SelectedFoodstuffsSnackbar(fragmentView);
        searchView = fragmentView.findViewById(R.id.floating_search_view);

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
                if (topAdapterChild.containsFoodstuffWithId(edited.getId())) {
                    topAdapterChild.replaceItem(edited);
                }
                foodstuffAdapterChild.replaceItem(edited);
            }

            @Override
            public void onFoodstuffDeleted(Foodstuff deleted) {
                foodstuffAdapterChild.removeItem(deleted);
            }

            @Override
            public void onFoodstuffsSavedToHistory() {
                bucketList.clear();
            }
        });

        snackbar.setOnBasketClickRunnable(() -> {
            BucketListActivity.start(new ArrayList<>(snackbar.getSelectedFoodstuffs()), context);
        });

        adapterParent = new AdapterParent();
        recyclerView.setAdapter(adapterParent);

        cardDialogOnAddFoodstuffButtonClickListener = foodstuff -> {
            hideCard();
            bucketList.add(foodstuff);
            snackbar.show();
            new KeyboardHandler(context).hideKeyBoard();
            searchView.clearQuery();
        };
        cardDialogOnEditButtonClickListener = foodstuff -> {
            EditFoodstuffActivity.startForEditing(fragment, foodstuff);
        };

        CardDialog cardDialog = CardDialog.findCard(context);
        if (cardDialog != null) {
            cardDialog.setUpAddFoodstuffButton(cardDialogOnAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            cardDialog.setOnEditButtonClickListener(cardDialogOnEditButtonClickListener);
        }

        topList.getTopList(foodstuffs -> {
            fillTop(foodstuffs);

            foodstuffsList.getAllFoodstuffs(batch -> {
                fillAllFoodstuffsList(batch);
            }, unused -> {
                configureSuggesionsDisplaying();

                configureSearch();
            });
        });
    }

    @Override
    public void onFragmentDestroy() {
        BucketList bucketList = BucketList.getInstance();
        bucketList.removeObserver(bucketListObserver);
    }

    private void fillAllFoodstuffsList(List<Foodstuff> batch) {
        if (foodstuffAdapterChild == null) {
            SingleItemAdapterChild.Observer observer = v -> {
                View addNewFoodstuffButton = v.findViewById(R.id.add_new_foodstuff);
                addNewFoodstuffButton.setOnClickListener(v1 -> {
                    EditFoodstuffActivity.startForCreation(context);
                });
            };
            SingleItemAdapterChild foodstuffsTitle = new SingleItemAdapterChild(
                    R.layout.all_foodstuffs_header, observer);
            foodstuffAdapterChild = new FoodstuffsAdapterChild(context, (foodstuff, pos) -> showCard(foodstuff));
            adapterParent.addChild(foodstuffsTitle);
            adapterParent.addChild(foodstuffAdapterChild);
        }
        foodstuffAdapterChild.addItems(batch);
    }

    private void fillTop(List<Foodstuff> foodstuffs) {
        if (!foodstuffs.isEmpty()) {
            if (topAdapterChild == null) {
                topAdapterChild = new FoodstuffsAdapterChild(context, (foodstuff, pos) -> showCard(foodstuff));
                SingleItemAdapterChild topTitle = new SingleItemAdapterChild(R.layout.top_foodstuffs_header);
                adapterParent.addChild(topTitle);
                adapterParent.addChild(topAdapterChild);
                topAdapterChild.addItems(foodstuffs);
            }
        }
    }

    private void configureSearch() {
        searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            // действие при нажатии на подсказку
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                FoodstuffSearchSuggestion suggestion = (FoodstuffSearchSuggestion) searchSuggestion;
                showCard(suggestion.getFoodstuff());
            }

            // когда пользователь нажал на клавиатуре enter
            @Override
            public void onSearchAction(String currentQuery) {
                SearchResultsFragment.show(currentQuery, context);
            }
        });

        // когда пользователь нажал кнопку лупы в searchView
        searchView.setOnMenuItemClickListener(item -> {
            SearchResultsFragment.show(searchView.getQuery(), context);
        });
    }

    private void configureSuggesionsDisplaying() {
        searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
            lastSearchDisposable.dispose();
            //get suggestions based on newQuery
            Single<List<Foodstuff>> searchResult = foodstuffsList.requestFoodstuffsLike(newQuery, SEARCH_SUGGESTIONS_NUMBER);
            lastSearchDisposable = searchResult.subscribe((result) -> {
                //pass them on to the search view
                List<FoodstuffSearchSuggestion> newSuggestions = new ArrayList<>();
                for (Foodstuff foodstuff : result) {
                    FoodstuffSearchSuggestion suggestion = new FoodstuffSearchSuggestion(foodstuff);
                    newSuggestions.add(suggestion);
                }
                searchView.swapSuggestions(newSuggestions);
            });
        });
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
        if (dialogAction != null) {
            dialogAction.run();
        }

        BucketList bucketList = BucketList.getInstance();
        snackbar.update(bucketList.getList());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                showCard(foodstuff);
            }
        } else if (requestCode == EDIT_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff editedFoodstuff = data.getParcelableExtra(EDIT_RESULT);
                topAdapterChild.replaceItem(editedFoodstuff);
                foodstuffAdapterChild.replaceItem(editedFoodstuff);
                showCard(editedFoodstuff);
            }
        }
    }

    private void showCard(Foodstuff foodstuff) {
        dialogAction = () -> {
            CardDialog cardDialog = CardDialog.showCard(context, foodstuff);
            cardDialog.setUpAddFoodstuffButton(cardDialogOnAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            cardDialog.setOnEditButtonClickListener(cardDialogOnEditButtonClickListener);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    private void hideCard() {
        dialogAction = () -> {
            CardDialog.hideCard(context);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }


}
