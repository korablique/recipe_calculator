package korablique.recipecalculator.util;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.ArrayRes;
import androidx.annotation.Nullable;
import korablique.recipecalculator.ui.ArrayAdapterWithDisabledItem;
import korablique.recipecalculator.ui.usergoal.RobotoMonoArrayAdapter;

/**
 * Utility class for pretty-tuning spinners, for example:
 *     startTuningSpinner(findViewById(R.id.gender_spinner))
 *             .withItems(R.array.gender_array)
 *             .addDisabledItemAt(0)
 *             .tune();
 */
public class SpinnerTuner {
    public interface OnItemSelectedListener {
        void onItemSelected(int position, long id);
    }

    private SpinnerTuner() {
    }

    public static Step1 startTuningSpinner(Spinner spinner) {
        return new Step1(spinner);
    }

    /**
     * First tuning step with a necessary items parameter.
     */
    public static class Step1 {
        private Spinner spinner;

        private Step1(Spinner spinner) {
            this.spinner = spinner;
        }

        public StepFinal withItems(@ArrayRes int arrayRes) {
            return new StepFinal(spinner, arrayRes);
        }
    }

    /**
     * Final tuning step with not necessary parameters (listeners,
     * disabled item index, ...).
     */
    public static class StepFinal {
        private Spinner spinner;
        @ArrayRes
        private int arrayRes;
        @Nullable
        private Integer disabledItemIndex;
        @Nullable
        private OnItemSelectedListener onItemSelectedListener;

        private StepFinal(Spinner spinner, @ArrayRes int arrayRes) {
            this.spinner = spinner;
            this.arrayRes = arrayRes;
        }

        public StepFinal addDisabledItemAt(int index) {
            disabledItemIndex = index;
            return this;
        }

        public StepFinal onItemSelected(OnItemSelectedListener onItemSelectedListener) {
            this.onItemSelectedListener = onItemSelectedListener;
            return this;
        }

        public void tune() {
            Context context = spinner.getContext();
            List<String> itemsList = new ArrayList<>(
                    Arrays.asList(context.getResources().getStringArray(arrayRes)));
            ArrayAdapter<String> adapter;
            if (disabledItemIndex != null) {
                adapter = new ArrayAdapterWithDisabledItem(
                        context, android.R.layout.simple_spinner_item,
                        itemsList, disabledItemIndex);
            } else {
                adapter = new RobotoMonoArrayAdapter(
                        context, android.R.layout.simple_spinner_item, itemsList);
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (onItemSelectedListener != null) {
                        onItemSelectedListener.onItemSelected(position, id);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Not used
                }
            });
        }
    }
}
