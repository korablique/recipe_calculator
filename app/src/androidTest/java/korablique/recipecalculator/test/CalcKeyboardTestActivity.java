package korablique.recipecalculator.test;

import android.os.Bundle;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.ui.calckeyboard.CalcEditText;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.numbersediting.EditProgressText;

public class CalcKeyboardTestActivity extends BaseActivity {
    @Inject
    CalcKeyboardController calcKeyboardController;

    @Override
    protected Integer getLayoutId() {
        return R.layout.calc_keyboard_test_activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalcEditText calcEditText = findViewById(R.id.calc_edit_text);
        EditProgressText editProgressText = findViewById(R.id.edit_progress_text);
        calcKeyboardController.useCalcKeyboardWith(calcEditText, this);
        calcKeyboardController.useCalcKeyboardWith(editProgressText, this);
    }
}
