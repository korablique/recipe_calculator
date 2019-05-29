package korablique.recipecalculator.ui;

import android.text.Editable;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import korablique.recipecalculator.base.Callback;

public class TextWatcherAfterTextChangedAdapter extends TextWatcherAdapter {
    private Callback<Editable> callback;

    public TextWatcherAfterTextChangedAdapter(Callback<Editable> callback) {
        this.callback = callback;
    }

    @Override
    public void afterTextChanged(Editable s) {
        callback.onResult(s);
    }
}
