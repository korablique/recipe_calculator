package korablique.recipecalculator;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.Date;

public class HistoryActivity extends MyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        HistoryAdapter adapter = new HistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 5));
        adapter.addItem(new Foodstuff("тортик", 100, 5, 20, 50, 400), new Date(117, 7, 3));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 6));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 6, 31));
        adapter.addItem(new Foodstuff("тортик", 100, 5, 20, 50, 400), new Date(117, 7, 4));
        adapter.addItem(new Foodstuff("помидорка", 100, 0.5, 0.2, 4, 20), new Date(117, 7, 3));
    }
}
