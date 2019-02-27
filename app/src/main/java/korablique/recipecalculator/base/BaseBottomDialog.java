package korablique.recipecalculator.base;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import korablique.recipecalculator.R;

public class BaseBottomDialog extends DialogFragment {
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog1 = super.onCreateDialog(savedInstanceState);
        dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE); // TODO: 27.02.19 эта строчка нужна для android 5+?
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
}
