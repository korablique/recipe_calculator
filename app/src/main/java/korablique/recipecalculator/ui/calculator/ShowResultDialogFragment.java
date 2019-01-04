package korablique.recipecalculator.ui.calculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.DbHelper;
import korablique.recipecalculator.ui.KeyboardHandler;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;

public class ShowResultDialogFragment extends DialogFragment {
    private EditText recipeName;

    @Override
    @NonNull public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        double resultWeight = getArguments().getDouble(CalculatorActivity.WEIGHT);
        final double proteinPer100Gram = getArguments().getDouble(CalculatorActivity.PROTEIN);
        final double fatsPer100Gram = getArguments().getDouble(CalculatorActivity.FATS);
        final double carbsPer100Gram = getArguments().getDouble(CalculatorActivity.CARBS);
        final double caloriesPer100Gram = getArguments().getDouble(CalculatorActivity.CALORIES);
        String dialogText = getString(R.string.calc_result_dialog_text,
                resultWeight,
                proteinPer100Gram,
                fatsPer100Gram,
                carbsPer100Gram,
                caloriesPer100Gram);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);
        recipeName = new EditText(getContext());
        recipeName.setHint(R.string.recipe_name);
        layout.addView(recipeName);
        TextView result = new TextView(getContext());
        result.setText(dialogText);
        layout.addView(result);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            }).setNeutralButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_NAME_FOODSTUFF_NAME, recipeName.getText().toString());
                    values.put(COLUMN_NAME_PROTEIN, proteinPer100Gram);
                    values.put(COLUMN_NAME_FATS, fatsPer100Gram);
                    values.put(COLUMN_NAME_CARBS, carbsPer100Gram);
                    values.put(COLUMN_NAME_CALORIES, caloriesPer100Gram);
                    DbHelper dbHelper = new DbHelper(getContext());
                    SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                    database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                    Snackbar.make(getActivity().findViewById(android.R.id.content),
                            "Продукт сохранён", Snackbar.LENGTH_SHORT).show();
                }
            });
        builder.setView(layout);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);

        recipeName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                }
            }
        });
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        new KeyboardHandler(getActivity()).hideKeyBoard();
    }
}