package korablique.recipecalculator.ui.card;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.card.NewCard;
import korablique.recipecalculator.ui.mainscreen.MainScreenActivity;


public class CardDialog extends DialogFragment {
    private NewCard card;
    private NewCard.OnAddFoodstuffButtonClickListener onAddFoodstuffButtonClickListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        card = new NewCard(getContext(), container);
        card.setOnAddFoodstuffButtonClickListener(onAddFoodstuffButtonClickListener);
        Foodstuff foodstuff = getArguments().getParcelable(MainScreenActivity.CLICKED_FOODSTUFF);
        card.setFoodstuff(foodstuff);

        return card.getCardLayout();
    }

    @Override
    @NonNull public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog1 = super.onCreateDialog(savedInstanceState);
        dialog1.setOnShowListener(dialog -> {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog1.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            dialog1.getWindow().setAttributes(layoutParams);
            dialog1.getWindow().setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.new_card_background));
        });
        return dialog1;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    public void setOnAddFoodstuffButtonClickListener(NewCard.OnAddFoodstuffButtonClickListener listener) {
        if (card != null) {
            card.setOnAddFoodstuffButtonClickListener(listener);
        } else {
            this.onAddFoodstuffButtonClickListener = listener;
        }
    }
}
