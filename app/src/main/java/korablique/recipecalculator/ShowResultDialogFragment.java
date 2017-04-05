package korablique.recipecalculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ShowResultDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String resultString = getArguments().getString(CalculatorActivity.RESULT_STRING);
        builder.setMessage(resultString)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ShowResultDialogFragment.this.getDialog().cancel(); //или надо dialog из аргументов использовать?
                }
            });
        return builder.create();
    }
}