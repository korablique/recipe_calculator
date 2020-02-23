package korablique.recipecalculator.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import org.joda.time.DateTime;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.model.UserParameters;

public class TwoOptionsDialog extends BaseBottomDialog {
    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_POSITIVE_BUTTON_TEXT = "EXTRA_POSITIVE_BUTTON_TEXT";
    private static final String EXTRA_NEGATIVE_BUTTON_TEXT = "EXTRA_NEGATIVE_BUTTON_TEXT";

    public enum ButtonName {
        POSITIVE,
        NEGATIVE
    }

    public interface ButtonsClickListener {
        void onClick(ButtonName buttonName);
    }

    @Nullable
    private ButtonsClickListener buttonsClickListener;
    @Nullable
    private Runnable dismissListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.two_options_dialog, container);

        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalArgumentException(getTag() + " doesn't have arguments somehow!");
        }

        TextView title = dialogLayout.findViewById(R.id.title);
        title.setText(args.getString(EXTRA_TITLE));
        Button positiveButton = dialogLayout.findViewById(R.id.positive_button);
        positiveButton.setText(args.getString(EXTRA_POSITIVE_BUTTON_TEXT));
        Button negativeButton = dialogLayout.findViewById(R.id.negative_button);
        negativeButton.setText(args.getString(EXTRA_NEGATIVE_BUTTON_TEXT));

        positiveButton.setOnClickListener((unused) -> {
            if (buttonsClickListener != null) {
                buttonsClickListener.onClick(ButtonName.POSITIVE);
            }
        });
        negativeButton.setOnClickListener((unused) -> {
            if (buttonsClickListener != null) {
                buttonsClickListener.onClick(ButtonName.NEGATIVE);
            }
        });

        return dialogLayout;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.run();
        }
    }

    public void setOnButtonsClickListener(ButtonsClickListener listener) {
        this.buttonsClickListener = listener;
    }

    public void setOnDismissListener(Runnable listener) {
        this.dismissListener = listener;
    }

    public static TwoOptionsDialog showDialog(
            FragmentManager fragmentManager,
            String fragmentTag,
            String title,
            String positiveButtonText,
            String negativeButtonText) {
        TwoOptionsDialog dialog = new TwoOptionsDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putString(EXTRA_NEGATIVE_BUTTON_TEXT, negativeButtonText);
        dialog.setArguments(args);
        dialog.show(fragmentManager, fragmentTag);
        return dialog;
    }

    @Nullable
    public static TwoOptionsDialog findDialog(FragmentManager fragmentManager, String fragmentTag) {
        return (TwoOptionsDialog) fragmentManager.findFragmentByTag(fragmentTag);
    }
}
