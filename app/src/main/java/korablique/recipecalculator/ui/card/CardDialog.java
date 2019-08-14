package korablique.recipecalculator.ui.card;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class CardDialog extends BaseBottomDialog {
    private static final String CLICKED_FOODSTUFF = "CLICKED_FOODSTUFF";
    private static final String CLICKED_WEIGHTED_FOODSTUFF = "CLICKED_WEIGHTED_FOODSTUFF";
    private static final String FOODSTUFF_CARD = "FOODSTUFF_CARD";
    private Card card;
    private Card.OnAddFoodstuffButtonClickListener onAddFoodstuffButtonClickListener;
    @StringRes
    private int buttonTextRes;
    private Card.OnEditButtonClickListener onEditButtonClickListener;
    private Card.OnCloseButtonClickListener onCloseButtonClickListener = this::dismiss;
    private Card.OnDeleteButtonClickListener onDeleteButtonClickListener;
    private boolean prohibitEditingFlag;
    private boolean prohibitDeletingFlag;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        card = new Card(getContext(), container);
        card.setUpAddFoodstuffButton(onAddFoodstuffButtonClickListener, buttonTextRes);
        card.setOnEditButtonClickListener(onEditButtonClickListener);
        card.setOnCloseButtonClickListener(onCloseButtonClickListener);
        card.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
        card.prohibitEditing(prohibitEditingFlag);
        card.prohibitDeleting(prohibitDeletingFlag);

        Bundle args = getArguments();
        if (args.containsKey(CLICKED_WEIGHTED_FOODSTUFF)) {
            WeightedFoodstuff foodstuff = args.getParcelable(CLICKED_WEIGHTED_FOODSTUFF);
            card.setFoodstuff(foodstuff);
        } else {
            Foodstuff foodstuff = args.getParcelable(CLICKED_FOODSTUFF);
            card.setFoodstuff(foodstuff);
        }

        return card.getCardLayout();
    }

    public void setUpAddFoodstuffButton(
            Card.OnAddFoodstuffButtonClickListener listener, @StringRes int buttonTextRes) {
        onAddFoodstuffButtonClickListener = listener;
        this.buttonTextRes = buttonTextRes;
        if (card != null) {
            card.setUpAddFoodstuffButton(listener, buttonTextRes);
        }
    }

    public void setOnEditButtonClickListener(Card.OnEditButtonClickListener listener) {
        onEditButtonClickListener = listener;
        if (card != null) {
            card.setOnEditButtonClickListener(listener);
        }
    }

    public void setOnCloseButtonClickListener(Card.OnCloseButtonClickListener listener) {
        onCloseButtonClickListener = listener;
        if (card != null) {
            card.setOnCloseButtonClickListener(listener);
        }
    }

    public void setOnDeleteButtonClickListener(Card.OnDeleteButtonClickListener listener) {
        onDeleteButtonClickListener = listener;
        if (card != null) {
            card.setOnDeleteButtonClickListener(listener);
        }
    }

    public static CardDialog showCard(FragmentActivity activity, WeightedFoodstuff foodstuff) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLICKED_WEIGHTED_FOODSTUFF, foodstuff);
        return createDialogWith(activity, bundle);
    }

    public static CardDialog showCard(FragmentActivity activity, Foodstuff foodstuff) {
        CardDialog existingDialog = findCard(activity);
        if (existingDialog != null) {
            existingDialog.card.setFoodstuff(foodstuff);
            existingDialog.card.prohibitEditing(false);
            return existingDialog;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLICKED_FOODSTUFF, foodstuff);
        return createDialogWith(activity, bundle);
    }

    @NonNull
    private static CardDialog createDialogWith(FragmentActivity activity, Bundle args) {
        CardDialog dialog = new CardDialog();
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), FOODSTUFF_CARD);
        return dialog;
    }

    public void prohibitEditing(boolean flag) {
        prohibitEditingFlag = flag;
        if (card != null) {
            card.prohibitEditing(flag);
        }
    }

    public void prohibitDeleting(boolean flag) {
        prohibitDeletingFlag = flag;
        if (card != null) {
            card.prohibitDeleting(flag);
        }
    }

    public static void hideCard(FragmentActivity activity) {
        CardDialog cardDialog = findCard(activity);
        if (cardDialog != null) {
            cardDialog.dismiss();
        }
    }
    // Метод нужен, чтоб обрабатывать такую ситуацию:
    // При смене конфигурации экрана (пересоздании активити) диалог уже может находиться в пересозданной активити.
    // Если он уже существует, то ему надо задать onAddFoodstuffButtonClickListener
    @Nullable public static CardDialog findCard(FragmentActivity activity) {
        return (CardDialog) activity.getSupportFragmentManager().findFragmentByTag(FOODSTUFF_CARD);
    }
}
