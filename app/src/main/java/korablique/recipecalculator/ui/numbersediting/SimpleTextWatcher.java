package korablique.recipecalculator.ui.numbersediting;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class SimpleTextWatcher <T extends EditText> implements TextWatcher {
    private final OnTextChangedListener<T> onTextChangedListener;
    private final T editText;

    public interface OnTextChangedListener  <T extends EditText> {
        void onTextChanged(T editText);
    }

    public SimpleTextWatcher(T editText, OnTextChangedListener<T> listener) {
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
