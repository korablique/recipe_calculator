package korablique.recipecalculator.ui.card;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;

import static korablique.recipecalculator.ui.mainscreen.MainScreenActivity.CLICKED_FOODSTUFF;


public class CardDialog extends DialogFragment {
    private static final String FOODSTUFF_CARD = "FOODSTUFF_CARD";
    private NewCard card;
    private NewCard.OnAddFoodstuffButtonClickListener onAddFoodstuffButtonClickListener;
    private NewCard.OnEditButtonClickListener onEditButtonClickListener;
    private boolean prohibitEditingFlag;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        card = new NewCard(getContext(), container);
        card.setOnAddFoodstuffButtonClickListener(onAddFoodstuffButtonClickListener);
        card.setOnEditButtonClickListener(onEditButtonClickListener);
        Foodstuff foodstuff = getArguments().getParcelable(CLICKED_FOODSTUFF);
        card.setFoodstuff(foodstuff);
        card.prohibitEditing(prohibitEditingFlag);

        return card.getCardLayout();
    }

    @Override
    @NonNull public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog1 = super.onCreateDialog(savedInstanceState);
        dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog1.setOnShowListener(dialog -> {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog1.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            dialog1.getWindow().setAttributes(layoutParams);
            dialog1.getWindow().setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.new_card_background));
        });
        dialog1.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog1;
    }

    public void setOnAddFoodstuffButtonClickListener(NewCard.OnAddFoodstuffButtonClickListener listener) {
        if (card != null) {
            card.setOnAddFoodstuffButtonClickListener(listener);
        } else {
            this.onAddFoodstuffButtonClickListener = listener;
        }
    }

    public void setOnEditButtonClickListener(NewCard.OnEditButtonClickListener listener) {
        if (card != null) {
            card.setOnEditButtonClickListener(listener);
        } else {
            this.onEditButtonClickListener = listener;
        }
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
        CardDialog dialog = new CardDialog();
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(), FOODSTUFF_CARD);
        return dialog;
    }

    public void prohibitEditing(boolean flag) {
        if (card != null) {
            card.prohibitEditing(flag);
        } else {
            prohibitEditingFlag = flag;
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
    // Если он уже существует, то ему надо задать setOnAddFoodstuffButtonClickListener
    @Nullable public static CardDialog findCard(FragmentActivity activity) {
        return (CardDialog) activity.getSupportFragmentManager().findFragmentByTag(FOODSTUFF_CARD);
    }
}
