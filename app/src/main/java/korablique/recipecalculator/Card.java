package korablique.recipecalculator;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;

public class Card {
    private static long duration = 500L;
    private ViewGroup cardLayout;
    private Foodstuff editedFoodstuff;
    private int editedFoodstuffPosition;
    private boolean isDisplayed;
    private ValueAnimator animator;
    private float lastAnimatorDestination;
    private Float lastParentVisibleHeight;

    public Card(Activity activity, ViewGroup parentLayout) {
        cardLayout = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.card_layout, null);
        parentLayout.addView(cardLayout);

        // NOTE: тут происходит какая-то чёрная магия
        // (Когда карточку добавили в parentView, карточка внезапно становится высотой match_parent,
        // а если parent'а карточки передать в inflate, то происходит какая-то непонятная фигня).
        ViewGroup.LayoutParams params = cardLayout.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        cardLayout.setLayoutParams(params);

        Window rootWindow = activity.getWindow();
        View rootView = rootWindow.getDecorView().findViewById(android.R.id.content);
        //чтобы когда появляется клавиатура - карточка была над ней:
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        // сравнить предыдущую и текущую высоту видимой части родителя.
                        // если они разные - остановить анимацию, телепортировать карточку на эту разницу (+),
                        // продолжить анимацию (создать новый аниматор, задать конечный float = getYForDisplayedState()
                        // и задать оставшееся время).
                        if (lastParentVisibleHeight == null) {
                            lastParentVisibleHeight = (float) getVisibleParentHeight();
                        } else {
                            float currentParentVisibleHeight = getVisibleParentHeight();
                            if (currentParentVisibleHeight != lastParentVisibleHeight) {
                                Crashlytics.log("currentParentVisibleHeight = " + currentParentVisibleHeight + "; " +
                                        "lastParentVisibleHeight = " + lastParentVisibleHeight);
                                float visibleParentHeightDelta = currentParentVisibleHeight - lastParentVisibleHeight;
                                cardLayout.setY(cardLayout.getY() + visibleParentHeightDelta);
                                lastParentVisibleHeight = currentParentVisibleHeight;
                                Crashlytics.log("animator != null: " + (animator != null));
                                if (animator != null) {
                                    Crashlytics.log("animator.isStarted() = " + animator.isStarted());
                                }
                                if (animator != null && animator.isStarted()) {
                                    Crashlytics.log("animator.getDuration() = " + animator.getDuration());
                                    Crashlytics.log("animator.getCurrentPlayTime() = " + animator.getCurrentPlayTime());
                                    long duration = animator.getDuration() - animator.getCurrentPlayTime();
                                    Crashlytics.log("new duration = " + duration);
                                    animateCard(cardLayout.getY(), lastAnimatorDestination + visibleParentHeightDelta, duration);
                                }
                            }
                        }
                    }
                });
        cardLayout.setVisibility(View.INVISIBLE);
        cardLayout.post(new Runnable() {
            @Override
            public void run() {
                cardLayout.setY(getYForHiddenState());
                // setY() надо вызывать ПОСЛЕ того, как полностью развернется разметка карточки.
                // Если сделать setY() ДО этого, то карточка сначала будет помещена в указанный Y,
                // а потом, когда разметка будет полностью развернута, карточка поместится в положение (0; 0),
                // поэтому надо дождаться полного развертывания разметки
                cardLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private float getYForHiddenState() {
        /*
         *  |-----------|
         *  |           | <--- status bar
         *  |-----------| <--- parent.y
         *  |           |
         *  |           |
         *  |           |
         *  |           | <--- parent
         *  |           |
         *  |           |
         *  |           |
         *  |-----------| <--- getYForHiddenState()
         */
        return getVisibleParentHeight() + ((View) cardLayout.getParent()).getTranslationY();
    }

    private float getYForDisplayedState() {
        return getYForHiddenState() - cardLayout.getHeight();
    }

    public void displayEmpty() {
        Crashlytics.log("Card.displayEmpty");
        this.clear();
        getButtonDelete().setVisibility(View.GONE);
        cardLayout.bringToFront();

        animateCard(cardLayout.getY(), getYForDisplayedState(), duration);
        isDisplayed = true;
        editedFoodstuff = null;
    }

    private void animateCard(float startValue, float endValue, long duration) {
        Crashlytics.log("Card.animateCard");
        if (duration < 0) {
            Crashlytics.logException(new IllegalArgumentException("duration is negative: " + duration));
            duration = 0;
        }
        lastAnimatorDestination = endValue;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(startValue, endValue);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                cardLayout.setTranslationY(animatedValue);
            }
        });
        animator.start();
    }

    public void displayForFoodstuff(Foodstuff foodstuff, int position) {
        Crashlytics.log("Card.displayForFoodstuff");
        this.clear();
        displayEmpty();
        getButtonDelete().setVisibility(View.VISIBLE);
        getNameEditText().setText(foodstuff.getName());
        getWeightEditText().setText(String.valueOf(foodstuff.getWeight()));
        getProteinEditText().setText(String.valueOf(foodstuff.getProtein()));
        getFatsEditText().setText(String.valueOf(foodstuff.getFats()));
        getCarbsEditText().setText(String.valueOf(foodstuff.getCarbs()));
        getCaloriesEditText().setText(String.valueOf(foodstuff.getCalories()));
        editedFoodstuff = foodstuff;
        editedFoodstuffPosition = position;
    }

    public void hide() {
        Crashlytics.log("Card.hide");
        animateCard(cardLayout.getY(), getYForHiddenState(), duration);
        isDisplayed = false;
        editedFoodstuff = null;
        for (int index = 0; index < cardLayout.getChildCount(); index++) {
            cardLayout.getChildAt(index).clearFocus();
        }
    }

    private int getVisibleParentHeight() {
        View cardParent = (View) cardLayout.getParent();
        Rect visibleRect = new Rect();
        cardParent.getGlobalVisibleRect(visibleRect);
        return visibleRect.height();
    }

    private void clear() {
        getNameEditText().setText("");
        getWeightEditText().setText("");
        getProteinEditText().setText("");
        getFatsEditText().setText("");
        getCarbsEditText().setText("");
        getCaloriesEditText().setText("");
    }

    public boolean areAllEditTextsFull() {
        if (getNameEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getWeightEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getProteinEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getFatsEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getCarbsEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getCaloriesEditText().getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isFilledEnoughToSaveFoodstuff() {
        if (getNameEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getProteinEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getFatsEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getCarbsEditText().getText().toString().isEmpty()) {
            return false;
        }
        if (getCaloriesEditText().getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    public Foodstuff parseFoodstuff() throws NumberFormatException {
        String productName = getNameEditText().getText().toString().trim();
        double weight;
        if (getWeightEditText().getText().toString().isEmpty()) {
            weight = -1;
        } else {
            weight = Double.parseDouble(getWeightEditText().getText().toString());
        }
        double protein = Double.parseDouble(getProteinEditText().getText().toString());
        double fats = Double.parseDouble(getFatsEditText().getText().toString());
        double carbs = Double.parseDouble(getCarbsEditText().getText().toString());
        double calories = Double.parseDouble(getCaloriesEditText().getText().toString());
        return new Foodstuff(productName, weight, protein, fats, carbs, calories);
    }

    public Foodstuff getEditedFoodstuff() {
        return editedFoodstuff;
    }

    public int getEditedFoodstuffPosition() {
        if (editedFoodstuff == null) {
            throw new IllegalStateException();
        }
        return editedFoodstuffPosition;
    }

    //setOnButtonOkClickedRunnable() сделать и передавать его в listener
    public Button getButtonOk() {
        return (Button) cardLayout.findViewById(R.id.button_ok);
    }

    public Button getButtonDelete() {
        return (Button) cardLayout.findViewById(R.id.button_delete);
    }

    public Button getButtonSave() {
        return (Button) cardLayout.findViewById(R.id.button_save);
    }

    public EditText getNameEditText() {
        return (EditText) cardLayout.findViewById(R.id.name_edit_text);
    }

    public EditText getProteinEditText() {
        return (EditText) cardLayout.findViewById(R.id.protein_edit_text);
    }

    public EditText getFatsEditText() {
        return (EditText) cardLayout.findViewById(R.id.fats_edit_text);
    }

    public EditText getCarbsEditText() {
        return (EditText) cardLayout.findViewById(R.id.carbs_edit_text);
    }

    public EditText getCaloriesEditText() {
        return (EditText) cardLayout.findViewById(R.id.calories_edit_text);
    }

    public EditText getWeightEditText() {
        return (EditText) cardLayout.findViewById(R.id.weight_edit_text);
    }

    public View getSearchImageButton() {
        return cardLayout.findViewById(R.id.search_icon_layout);
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    public static void setAnimationDuration(long duration) {
        Card.duration = duration;
    }
}