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
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;

import static android.app.Activity.RESULT_OK;

@FragmentScope
public class MainScreenCardController extends FragmentCallbacks.Observer {
    @StringRes
    private static final int CARD_BUTTON_TEXT_RES = R.string.add_foodstuff;
    private final BaseActivity context;
    private final BaseFragment fragment;
    private final Lifecycle lifecycle;
    private final FoodstuffsList foodstuffsList;
    private final BucketList bucketList;
    private Card.OnAddFoodstuffButtonClickListener cardDialogOnAddFoodstuffButtonClickListener;
    private Card.OnEditButtonClickListener cardDialogOnEditButtonClickListener;

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
            FoodstuffsList foodstuffsList) {
        this.context = context;
        this.fragment = fragment;
        this.lifecycle = lifecycle;
        this.foodstuffsList = foodstuffsList;
        this.bucketList = BucketList.getInstance();
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        cardDialogOnAddFoodstuffButtonClickListener = foodstuff -> {
            hideCard();
            new KeyboardHandler(context).hideKeyBoard();
            bucketList.add(foodstuff);
        };
        cardDialogOnEditButtonClickListener = foodstuff -> {
            EditFoodstuffActivity.startForEditing(fragment, foodstuff, RequestCodes.MAIN_SCREEN_CARD_EDIT_FOODSTUFF);
        };

        CardDialog cardDialog = CardDialog.findCard(context);
        if (cardDialog != null) {
            cardDialog.setUpAddFoodstuffButton(cardDialogOnAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            cardDialog.setOnEditButtonClickListener(cardDialogOnEditButtonClickListener);
        }
    }

    @Override
    public void onFragmentResume() {
        if (dialogAction != null) {
            dialogAction.run();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            cardDialog.setUpAddFoodstuffButton(cardDialogOnAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            cardDialog.setOnEditButtonClickListener(cardDialogOnEditButtonClickListener);
            cardDialog.prohibitDeleting(true);
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
}
