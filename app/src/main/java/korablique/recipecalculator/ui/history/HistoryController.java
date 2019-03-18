package korablique.recipecalculator.ui.history;

import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.ui.mainscreen.MainScreenFragment;

@FragmentScope
public class HistoryController extends FragmentCallbacks.Observer {
    private HistoryWorker historyWorker;
    private BaseActivity context;
    private HistoryFragment fragment;

    @Inject
    public HistoryController(
            BaseActivity context,
            HistoryFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            HistoryWorker historyWorker) {
        fragmentCallbacks.addObserver(this);
        this.context = context;
        this.fragment = fragment;
        this.historyWorker = historyWorker;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView) {
        FloatingActionButton fab = fragmentView.findViewById(R.id.history_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainScreenFragment.show(context);
            }
        });

        RecyclerView recyclerView = fragmentView.findViewById(R.id.history_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        NewHistoryAdapter adapter = new NewHistoryAdapter(fragmentView.getContext());
        recyclerView.setAdapter(adapter);
        DividerItemDecorationWithoutDividerAfterLastItem dividerItemDecoration = new DividerItemDecorationWithoutDividerAfterLastItem(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(fragmentView.getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        DateTime today = DateTime.now();
        DateTime todayMidnight = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 0, 0, 0);
        DateTime todayEnd = new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 23, 59, 59);
        historyWorker.requestHistoryForPeriod(
                todayMidnight.getMillis(),
                todayEnd.getMillis(), new HistoryWorker.RequestHistoryCallback() {
            @Override
            public void onResult(List<HistoryEntry> historyEntries) {
                adapter.addItems(historyEntries);
            }
        });
    }
}
