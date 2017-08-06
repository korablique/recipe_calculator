package korablique.recipecalculator;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Date;

import static korablique.recipecalculator.IntentConstants.FIND_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.NAME;
import static korablique.recipecalculator.IntentConstants.SEARCH_RESULT;


public class HistoryActivity extends MyActivity {
    private Card card;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private HistoryAdapter.Observer adapterObserver = new HistoryAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            card.displayForFoodstuff(foodstuff, position);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        adapter = new HistoryAdapter(adapterObserver);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 5));
        adapter.addItem(new Foodstuff("тортик", 100, 5, 20, 50, 400), new Date(117, 7, 3));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 6));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 6, 31));
        adapter.addItem(new Foodstuff("тортик", 100, 5, 20, 50, 400), new Date(117, 7, 4));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 3));

        ViewGroup parentLayout = (ViewGroup) findViewById(R.id.history_parent);
        card = new Card(this, parentLayout);

        FloatingActionButton historyFab = (FloatingActionButton) findViewById(R.id.history_fab);
        historyFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                card.displayEmpty();
            }
        });

        View cardsButtonOK = card.getButtonOk();
        cardsButtonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!card.areAllEditTextsFull()) {
                    Toast.makeText(HistoryActivity.this, "Заполните все данные", Toast.LENGTH_SHORT).show();
                    return;
                }

                Foodstuff foodstuff;
                try {
                    foodstuff = card.parseFoodstuff();
                } catch (NumberFormatException e) {
                    Toast.makeText(HistoryActivity.this, "В полях для ввода БЖУК вводите только числа", Toast.LENGTH_LONG).show();
                    return;
                }

                Foodstuff editedFoodstuff = card.getEditedFoodstuff();
                if (editedFoodstuff == null) {
                    adapter.addItem(foodstuff, new Date());
                    recyclerView.smoothScrollToPosition(1); //т к новый продукт добавляется в текущую дату
                } else {
                    adapter.replaceItem(foodstuff, card.getEditedFoodstuffPosition());
                    recyclerView.smoothScrollToPosition(card.getEditedFoodstuffPosition());
                }

                card.hide();
                KeyboardHandler keyboardHandler = new KeyboardHandler(HistoryActivity.this);
                keyboardHandler.hideKeyBoard();
            }
        });

        View cardsButtonDelete = card.getButtonDelete();
        cardsButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.deleteItem(card.getEditedFoodstuffPosition());
                card.hide();
            }
        });

        View cardsButtonSave = card.getButtonSave();
        cardsButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!card.isFilledEnoughToSaveFoodstuff()) {
                    Toast.makeText(HistoryActivity.this, "Заполните название и БЖУК", Toast.LENGTH_LONG).show();
                    return;
                }

                Foodstuff savingFoodstuff = card.parseFoodstuff();

                if (savingFoodstuff.getProtein() + savingFoodstuff.getFats() + savingFoodstuff.getCarbs() > 100) {
                    Toast.makeText(
                            HistoryActivity.this,
                            "Сумма белков, жиров и углеводов не может быть больше 100",
                            Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
                databaseWorker.saveFoodstuff(HistoryActivity.this, savingFoodstuff);
            }
        });

        View cardsSearchImageButton = card.getSearchImageButton();
        cardsSearchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(HistoryActivity.this, ListOfFoodstuffsActivity.class);
                sendIntent.setAction(getString(R.string.find_foodstuff_action));
                String foodstuffName = card.getNameEditText().getText().toString().trim();
                sendIntent.putExtra(NAME, foodstuffName);
                startActivityForResult(sendIntent, FIND_FOODSTUFF_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_FOODSTUFF_REQUEST) {
            if (resultCode == RESULT_OK) {
                Foodstuff foodstuff = data.getParcelableExtra(SEARCH_RESULT);
                card.getNameEditText().setText(foodstuff.getName());
                card.getProteinEditText().setText(String.valueOf(foodstuff.getProtein()));
                card.getFatsEditText().setText(String.valueOf(foodstuff.getFats()));
                card.getCarbsEditText().setText(String.valueOf(foodstuff.getCarbs()));
                card.getCaloriesEditText().setText(String.valueOf(foodstuff.getCalories()));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (card != null && card.isDisplayed()) {
            card.hide();
        } else {
            super.onBackPressed();
        }
    }
}
