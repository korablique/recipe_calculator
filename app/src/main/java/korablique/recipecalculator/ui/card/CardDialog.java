package korablique.recipecalculator.ui.card;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import static korablique.recipecalculator.ui.mainscreen.MainScreenActivity.FOODSTUFF_CARD;


public class CardDialog extends DialogFragment {
    private NewCard card;
    private NewCard.OnAddFoodstuffButtonClickListener onAddFoodstuffButtonClickListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        card = new NewCard(getContext(), container);
        card.setOnAddFoodstuffButtonClickListener(onAddFoodstuffButtonClickListener);
        Foodstuff foodstuff = getArguments().getParcelable(CLICKED_FOODSTUFF);
        card.setFoodstuff(foodstuff);

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

    public static void showCard(FragmentActivity activity, Foodstuff foodstuff) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLICKED_FOODSTUFF, foodstuff);
        CardDialog dialog = new CardDialog();
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(), FOODSTUFF_CARD);
    }
}
