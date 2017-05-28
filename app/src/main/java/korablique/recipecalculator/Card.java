package korablique.recipecalculator;

import android.app.Activity;
import android.graphics.Point;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Card {
    private Activity activity;
    private View cardLayout;
    private Foodstuff editedFoodstuff;
    private int editedFoodstuffPosition;
    private EditText nameEditText;
    private EditText weightEditText;
    private EditText proteinEditText;
    private EditText fatsEditText;
    private EditText carbsEditText;
    private EditText caloriesEditText;
    private Button buttonOk;
    private Button buttonDelete;
    private Button buttonSave;
    private boolean isDisplayed;

    public Card(Activity activity, ViewGroup parentLayout) {
        this.activity = activity;
        cardLayout = LayoutInflater.from(activity).inflate(R.layout.card_layout, null);
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

        nameEditText = (EditText) cardLayout.findViewById(R.id.name_edit_text);
        weightEditText = (EditText) cardLayout.findViewById(R.id.weight_edit_text);
        proteinEditText = (EditText) cardLayout.findViewById(R.id.protein_edit_text);
        fatsEditText = (EditText) cardLayout.findViewById(R.id.fats_edit_text);
        carbsEditText = (EditText) cardLayout.findViewById(R.id.carbs_edit_text);
        caloriesEditText = (EditText) cardLayout.findViewById(R.id.calories_edit_text);
        buttonOk = (Button) cardLayout.findViewById(R.id.button_ok);
        buttonDelete = (Button) cardLayout.findViewById(R.id.button_delete);
        buttonSave = (Button) cardLayout.findViewById(R.id.button_save);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        cardLayout.setY(getVisibleParentHeight()
                                + ((View)cardLayout.getParent()).getY()
                                - cardLayout.getHeight());
                    }
                });
    }

    public void displayEmpty() {
        this.clear();
        buttonDelete.setVisibility(View.GONE);
        cardLayout.setVisibility(View.VISIBLE);
        cardLayout.bringToFront();
        /*TranslateAnimation translateAnimation =
                new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0,
                        Animation.RELATIVE_TO_PARENT, 0,
                        Animation.RELATIVE_TO_PARENT, parentLayout.getHeight(),
                        Animation.RELATIVE_TO_PARENT, parentLayout.getHeight() - cardLayout.getHeight());
        translateAnimation.setDuration(1000);
        translateAnimation.setFillAfter(true);
        cardLayout.startAnimation(translateAnimation);*/
        cardLayout.setY(getVisibleParentHeight() - cardLayout.getHeight());
        isDisplayed = true;
        editedFoodstuff = null;
    }

    public void displayForFoodstuff(Foodstuff foodstuff, int position) {
        this.clear();
        displayEmpty();
        buttonDelete.setVisibility(View.VISIBLE);
        nameEditText.setText(foodstuff.getName());
        weightEditText.setText(String.valueOf(foodstuff.getWeight()));
        proteinEditText.setText(String.valueOf(foodstuff.getProtein()));
        fatsEditText.setText(String.valueOf(foodstuff.getFats()));
        carbsEditText.setText(String.valueOf(foodstuff.getCarbs()));
        caloriesEditText.setText(String.valueOf(foodstuff.getCalories()));
        editedFoodstuff = foodstuff;
        editedFoodstuffPosition = position;
    }

    public void hide() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int displayHeight = size.y;
        cardLayout.setY(cardLayout.getHeight() + displayHeight);
        cardLayout.setVisibility(View.INVISIBLE);
        isDisplayed = false;
        editedFoodstuff = null;
    }

    private int getVisibleParentHeight() {
        View cardParent = (View)cardLayout.getParent();
        return cardParent.getHeight();
    }

    private void clear() {
        nameEditText.setText("");
        weightEditText.setText("");
        proteinEditText.setText("");
        fatsEditText.setText("");
        carbsEditText.setText("");
        caloriesEditText.setText("");
    }

    public boolean areAllEditTextsFull() {
        if (nameEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (weightEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (proteinEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (fatsEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (carbsEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (caloriesEditText.getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isFilledEnoughToSaveFoodstuff() {
        if (nameEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (proteinEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (fatsEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (carbsEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (caloriesEditText.getText().toString().isEmpty()) {
            return false;
        }
        return true;
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
        return buttonOk;
    }

    public Button getButtonDelete() {
        return buttonDelete;
    }

    public Button getButtonSave() {
        return buttonSave;
    }

    public EditText getNameEditText() {
        return nameEditText;
    }

    public EditText getProteinEditText() {
        return proteinEditText;
    }

    public EditText getFatsEditText() {
        return fatsEditText;
    }

    public EditText getCarbsEditText() {
        return carbsEditText;
    }

    public EditText getCaloriesEditText() {
        return caloriesEditText;
    }

    public EditText getWeightEditText() {
        return weightEditText;
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }
}
