package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.lifecycle.Lifecycle;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;

import static android.app.Activity.RESULT_OK;

@FragmentScope
public class MainScreenCardController implements FragmentCallbacks.Observer {
    private enum CardMode {
        NONE, // Пустой режим для непроинициализированной карточки
        DEFAULT, // Режим по-умолчанию, в карточке обе кнопки
        DISH_CREATION // Режим создания блюда, в карточке только кнопка добавления блюда
    }
    @StringRes
    private static final int ADD_FOODSTUFF_TO_RECIPE_CARD_TEXT = R.string.add_foodstuff_to_recipe;
    @StringRes
    private static final int ADD_FOODSTUFF_TO_HISTORY_CARD_TEXT = R.string.add_foodstuff_to_history;
    private final BaseActivity context;
    private final BaseFragment fragment;
    private final Lifecycle lifecycle;
    private final BucketList bucketList;
    private final HistoryWorker historyWorker;
    private final TimeProvider timeProvider;
    private final BucketList.Observer bucketListObserver = new BucketList.Observer() {
        @Override
        public void onFoodstuffAdded(WeightedFoodstuff wf) {
            // Первый продукт добавлен в бакетлист
            if (bucketList.getList().size() == 1) {
                switchCardMode(CardMode.DISH_CREATION);
            }
        }
        @Override
        public void onFoodstuffRemoved(WeightedFoodstuff wf) {
            // Последний продукт удален из бакетлиста
            if (bucketList.getList().isEmpty()) {
                switchCardMode(CardMode.DEFAULT);
            }
        }
    };
    private Card.OnMainButtonClickListener onAddFoodstuffToRecipeListener;
    private Card.OnMainButtonClickListener onAddFoodstuffToHistoryListener;
    private Card.OnEditButtonClickListener onEditButtonClickListener;
    private CardMode currentCardMode = CardMode.NONE;

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

    @Inject
    public MainScreenCardController(
            BaseActivity context,
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            Lifecycle lifecycle,
            HistoryWorker historyWorker,
            TimeProvider timeProvider) {
        this.context = context;
        this.fragment = fragment;
        this.lifecycle = lifecycle;
        this.bucketList = BucketList.getInstance();
        this.historyWorker = historyWorker;
        this.timeProvider = timeProvider;
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        onAddFoodstuffToRecipeListener = foodstuff -> {
            hideCard();
            new KeyboardHandler(context).hideKeyBoard();
            bucketList.add(foodstuff);
        };
        onAddFoodstuffToHistoryListener = foodstuff -> {
            hideCard();
            new KeyboardHandler(context).hideKeyBoard();
            historyWorker.saveFoodstuffToHistory(
                    timeProvider.now().toDate(), foodstuff.getId(), foodstuff.getWeight());
        };
        onEditButtonClickListener = foodstuff -> {
            EditFoodstuffActivity.startForEditing(fragment, foodstuff, RequestCodes.MAIN_SCREEN_CARD_EDIT_FOODSTUFF);
        };
        bucketList.addObserver(bucketListObserver);

        if (bucketList.getList().isEmpty()) {
            switchCardMode(CardMode.DEFAULT);
        } else {
            switchCardMode(CardMode.DISH_CREATION);
        }
    }

    @Override
    public void onFragmentDestroy() {
        bucketList.removeObserver(bucketListObserver);
    }

    @Override
    public void onFragmentResume() {
        if (dialogAction != null) {
            dialogAction.run();
        }
    }

    @Override
    public void onFragmentActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.MAIN_SCREEN_CARD_EDIT_FOODSTUFF
                && resultCode == RESULT_OK) {
            Foodstuff editedFoodstuff = data.getParcelableExtra(EditFoodstuffActivity.EXTRA_RESULT_FOODSTUFF);
            if (editedFoodstuff == null) {
                hideCard();
            } else {
                showCard(editedFoodstuff);
            }
        }
    }

    public void showCard(Foodstuff foodstuff) {
        dialogAction = () -> {
            CardDialog cardDialog = CardDialog.showCard(context, foodstuff);
            setUpCard(cardDialog);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    public void hideCard() {
        dialogAction = () -> {
            CardDialog.hideCard(context);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    private void switchCardMode(CardMode newMode) {
        if (newMode == currentCardMode) {
            return;
        }
        currentCardMode = newMode;
        CardDialog cardDialog = CardDialog.findCard(context);
        if (cardDialog != null) {
            setUpCard(cardDialog);
        }
    }

    private void setUpCard(CardDialog card) {
        card.setOnEditButtonClickListener(onEditButtonClickListener);
        card.prohibitDeleting(true);
        switch (currentCardMode) {
            case NONE:
                card.deinitButton1();
                card.deinitButton2();
                break;
            case DEFAULT:
                card.setUpButton1(onAddFoodstuffToHistoryListener, ADD_FOODSTUFF_TO_HISTORY_CARD_TEXT);
                card.setUpButton2(onAddFoodstuffToRecipeListener, ADD_FOODSTUFF_TO_RECIPE_CARD_TEXT);
                break;
            case DISH_CREATION:
                card.deinitButton1();
                card.setUpButton2(onAddFoodstuffToRecipeListener, ADD_FOODSTUFF_TO_RECIPE_CARD_TEXT);
                break;
            default:
                throw new IllegalStateException("Unhandled card mode: " + currentCardMode);
        }
    }
}
