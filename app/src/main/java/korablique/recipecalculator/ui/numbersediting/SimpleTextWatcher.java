package korablique.recipecalculator.ui.numbersediting;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

class SimpleTextWatcher <T extends EditText> implements TextWatcher {
    private final OnTextChangedListener<T> onTextChangedListener;
    private final T editText;

    interface OnTextChangedListener  <T extends EditText> {
        void onTextChanged(T editText);
    }

    SimpleTextWatcher(OnTextChangedListener<T> listener, T editText) {
        this.onTextChangedListener = listener;
        this.editText = editText;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        onTextChangedListener.onTextChanged(editText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable s) {}
}
