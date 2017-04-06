package korablique.recipecalculator;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class Card {
    private Context context;
    private FrameLayout parentLayout;
    private View cardLayout;
    private TableRow requiredRow;
    private EditText nameEditText;
    private EditText weightEditText;
    private EditText proteinEditText;
    private EditText fatsEditText;
    private EditText carbsEditText;
    private EditText caloriesEditText;
    private Button buttonOk;
    private Button buttonDelete;

    public Card(Context context, FrameLayout parentLayout) {
        this.context = context;
        this.parentLayout = parentLayout;
        cardLayout = LayoutInflater.from(context).inflate(R.layout.card_layout, null);
        parentLayout.addView(cardLayout);

        nameEditText = (EditText) cardLayout.findViewById(R.id.name_edit_text);
        weightEditText = (EditText) cardLayout.findViewById(R.id.weight_edit_text);
        proteinEditText = (EditText) cardLayout.findViewById(R.id.protein_edit_text);
        fatsEditText = (EditText) cardLayout.findViewById(R.id.fats_edit_text);
        carbsEditText = (EditText) cardLayout.findViewById(R.id.carbs_edit_text);
        caloriesEditText = (EditText) cardLayout.findViewById(R.id.calories_edit_text);
        buttonOk = (Button) cardLayout.findViewById(R.id.button_ok);
        buttonDelete = (Button) cardLayout.findViewById(R.id.button_delete);
    }

    public void displayEmpty() {
        // NOTE: тут происходит какая-то чёрная магия
        // (Когда карточку добавили в parentView, карточка внезапно становится высотой match_parent,
        // а если parent'а карточки передать в inflate, то происходит какая-то непонятная фигня,
        // а мне лень разбираться).
        ViewGroup.LayoutParams params = cardLayout.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        cardLayout.setLayoutParams(params);

        cardLayout.setVisibility(View.VISIBLE);
        cardLayout.bringToFront();
        cardLayout.setY(parentLayout.getHeight() - cardLayout.getHeight());
    }

    public void displayForRow(TableRow row) {
        displayEmpty();
        nameEditText.setText(((TextView) row.getChildAt(0)).getText().toString());
        weightEditText.setText(((TextView) row.getChildAt(1)).getText().toString());
        proteinEditText.setText(((TextView) row.getChildAt(2)).getText().toString());
        fatsEditText.setText(((TextView) row.getChildAt(3)).getText().toString());
        carbsEditText.setText(((TextView) row.getChildAt(4)).getText().toString());
        caloriesEditText.setText(((TextView) row.getChildAt(5)).getText().toString());
        requiredRow = row;
    }

    public void hide() {
        Display display = ((CalculatorActivity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int displayHeight = size.y;
        cardLayout.setY(cardLayout.getHeight() + displayHeight);
        cardLayout.setVisibility(View.INVISIBLE);
    }

    public void clear() {
        nameEditText.setText("");
        weightEditText.setText("");
        proteinEditText.setText("");
        fatsEditText.setText("");
        carbsEditText.setText("");
        caloriesEditText.setText("");
    }

    public View getCardLayout() {
        return cardLayout;
    }

    public TableRow getRequiredRow() {
        return requiredRow;
    }

    public void setRequiredRow(TableRow row) {
        requiredRow = row;
    }

    public Button getButtonOk() {
        return buttonOk;
    }

    public Button getButtonDelete() {
        return buttonDelete;
    }
}
