package korablique.recipecalculator.ui.numbersediting;

/**
 * Takes a group of {@link EditProgressText} and enforces a common max value among them
 * (so that their sum value would never be greater than commonMax).
 */
public class EditProgressTextCommonMaxController {
    private final float commonMax;
    private final EditProgressText[] views;

    public EditProgressTextCommonMaxController(float commonMax, EditProgressText... views) {
        this.commonMax = commonMax;
        this.views = views;
    }

    public void init() {
        for (EditProgressText view : views) {
            view.addTextChangedListener(new SimpleTextWatcher<>(this::onTextChanged, view));
        }
        updateCommonMaxes();
    }

    private void updateCommonMaxes() {
        for (EditProgressText currentView : views) {
            float otherViewsSumValue = getOtherViewsSumValue(currentView);
            currentView.setIntermediateMax(commonMax - otherViewsSumValue);
        }
    }

    private float getOtherViewsSumValue(EditProgressText view) {
        float otherViewsSumValue = 0.f;
        for (EditProgressText otherView : views) {
            if (otherView == view) {
                continue;
            }

            Float otherViewValue = otherView.getDisplayedNumber();
            if (otherViewValue == null) {
                otherViewValue = 0.f;
            }
            otherViewsSumValue += otherViewValue;
        }

        return otherViewsSumValue;
    }

    private void onTextChanged(EditProgressText view) {
        updateCommonMaxes();
    }
}
