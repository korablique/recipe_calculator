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

public class Card {
    private static long duration = 500L;
    private ViewGroup cardLayout;
    private boolean isDisplayed;
    private ValueAnimator animator;
    private float lastAnimatorDestination;
    private Float lastParentVisibleHeight;
    private Object customPayload; // здесь хранится id фудстаффа из списка

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
                                float visibleParentHeightDelta = currentParentVisibleHeight - lastParentVisibleHeight;
                                cardLayout.setY(cardLayout.getY() + visibleParentHeightDelta);
                                lastParentVisibleHeight = currentParentVisibleHeight;
                                if (animator != null && animator.isStarted()) {
                                    long duration = animator.getDuration() - animator.getCurrentPlayTime();
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
        this.clear();
        getButtonDelete().setVisibility(View.GONE);
        cardLayout.bringToFront();

        animateCard(cardLayout.getY(), getYForDisplayedState(), duration);
        isDisplayed = true;
    }

    private void animateCard(float startValue, float endValue, long duration) {
        if (duration < 0) {
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

    /**
     * id foodstuff'а не сохраняется
     */
    public void displayForFoodstuff(Foodstuff foodstuff, Object customPayload) {
        this.customPayload = customPayload;
        this.clear();
        displayEmpty();
        getButtonDelete().setVisibility(View.VISIBLE);
        setFoodstuff(foodstuff);
    }

    public void hide() {
        animateCard(cardLayout.getY(), getYForHiddenState(), duration);
        isDisplayed = false;
        for (int index = 0; index < cardLayout.getChildCount(); index++) {
            cardLayout.getChildAt(index).clearFocus();
        }
    }

    public void setFocusableExceptWeight(boolean focusable) {
        getNameEditText().setFocusable(focusable);
        getProteinEditText().setFocusable(focusable);
        getFatsEditText().setFocusable(focusable);
        getCarbsEditText().setFocusable(focusable);
        getCaloriesEditText().setFocusable(focusable);
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
        String productName = getName();
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

    public void setOnButtonOkClickedRunnable(final Runnable runnable) {
        setOnButtonClickedRunnable(runnable, R.id.button_ok);
    }

    public void setOnButtonDeleteClickedRunnable(final Runnable runnable) {
        setOnButtonClickedRunnable(runnable, R.id.button_delete);
    }

    public void setOnButtonSaveClickedRunnable(final Runnable runnable) {
        setOnButtonClickedRunnable(runnable, R.id.button_save);
    }

    public void setOnSearchButtonClickedRunnable(final Runnable runnable) {
        setOnButtonClickedRunnable(runnable, R.id.search_icon_layout);
    }

    private void setOnButtonClickedRunnable(final Runnable runnable, int buttonId) {
        cardLayout.findViewById(buttonId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runnable.run();
            }
        });
    }

    public void hideWeight() {
        getWeightEditText().setVisibility(View.GONE);
    }

    public void hideButtonOk() {
        getButtonOk().setVisibility(View.GONE);
    }

    public void hideSearchButton() {
        getSearchImageButton().setVisibility(View.GONE);
    }

    public Object getCurrentCustomPayload() {
        return customPayload;
    }

    public String getName() {
        return getNameEditText().getText().toString().trim();
    }

    private Button getButtonOk() {
        return (Button) cardLayout.findViewById(R.id.button_ok);
    }

    private Button getButtonDelete() {
        return (Button) cardLayout.findViewById(R.id.button_delete);
    }

    private EditText getNameEditText() {
        return (EditText) cardLayout.findViewById(R.id.name_edit_text);
    }

    private EditText getProteinEditText() {
        return (EditText) cardLayout.findViewById(R.id.protein_edit_text);
    }

    private EditText getFatsEditText() {
        return (EditText) cardLayout.findViewById(R.id.fats_edit_text);
    }

    private EditText getCarbsEditText() {
        return (EditText) cardLayout.findViewById(R.id.carbs_edit_text);
    }

    private EditText getCaloriesEditText() {
        return (EditText) cardLayout.findViewById(R.id.calories_edit_text);
    }

    private EditText getWeightEditText() {
        return (EditText) cardLayout.findViewById(R.id.weight_edit_text);
    }

    private View getSearchImageButton() {
        return cardLayout.findViewById(R.id.search_icon_layout);
    }

    public void setFoodstuff(Foodstuff newFoodstuff) {
        getNameEditText().setText(newFoodstuff.getName());
        if (newFoodstuff.getWeight() > 0) {
            getWeightEditText().setText(String.valueOf(newFoodstuff.getWeight()));
        } else {
            getWeightEditText().setText("");
        }
        getProteinEditText().setText(String.valueOf(newFoodstuff.getProtein()));
        getFatsEditText().setText(String.valueOf(newFoodstuff.getFats()));
        getCarbsEditText().setText(String.valueOf(newFoodstuff.getCarbs()));
        getCaloriesEditText().setText(String.valueOf(newFoodstuff.getCalories()));
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    public static void setAnimationDuration(long duration) {
        Card.duration = duration;
    }
}