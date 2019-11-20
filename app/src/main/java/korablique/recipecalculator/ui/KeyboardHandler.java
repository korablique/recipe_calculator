package korablique.recipecalculator.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class KeyboardHandler {
    private FragmentActivity activity;

    public KeyboardHandler(FragmentActivity activity) {
        this.activity = activity;
    }

    /**
     * Hides keyboard and clears focus.
     */
    public void hideKeyBoard() {
        hideKeyBoard(true);
    }

    /**
     * Hides keyboard but doesn't clear focus.
     */
    public void hideKeyBoardWithoutClearingFocus() {
        hideKeyBoard(false);
    }

    private void hideKeyBoard(boolean clearFocus) {
        View view = getCurrentFocus(activity);
        if (view != null) {
            if (clearFocus) {
                view.clearFocus();
            }
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            // For some views (FloatingSearchView) hiding of the keyboard doesn't in the same frame
            // in which we cleared focus from them. So we clear the focus in the next frame.
            view.post(() -> {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            });
        }
    }

    @Nullable
    private View getCurrentFocus(FragmentActivity activity) {
        // If any dialog is shown and it has a focused view -
        // system keyboard would be shown for that view in the dialog, not for Activity.
        // So we need to iterate over all fragments, find dialogs and check if they have a
        // focused view.
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof DialogFragment) {
                Dialog dialog = ((DialogFragment) fragment).getDialog();
                if (dialog != null && dialog.getCurrentFocus() != null) {
                    return dialog.getCurrentFocus();
                }
            }
        }
        return activity.getCurrentFocus();
    }
}
