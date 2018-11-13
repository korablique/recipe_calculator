package korablique.recipecalculator.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardHandler {
    private Activity activity;

    public KeyboardHandler(Activity activity) {
        this.activity = activity;
    }

    /**
     * Hides keyboard and clears focus.
     */
    public void hideKeyBoard() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            // For some views (FloatingSearchView) hiding of the keyboard doesn't in the same frame
            // in which we cleared focus from them. So we clear the focus in the next frame.
            view.post(() -> {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            });
        }
    }
}
