package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.dagger.FragmentScope;

/**
 * Controls the up FAB button located in the main screen.
 */
@FragmentScope
public class UpFABController extends FragmentCallbacks.Observer {
    private FloatingActionButton fab;
    private RecyclerView recyclerView;

    @Inject
    public UpFABController(FragmentCallbacks fragmentCallbacks) {
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        fab = fragmentView.findViewById(R.id.up_fab);
        recyclerView = fragmentView.findViewById(R.id.main_screen_recycler_view);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                onRecyclerViewScrolled();
            }
        });

        fab.setOnClickListener(v -> onFabClicked());

        onRecyclerViewScrolled();
    }

    private void onRecyclerViewScrolled() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
        boolean isFirstOrLastVisible = firstVisibleItem == 0
                || lastVisibleItem == recyclerView.getAdapter().getItemCount() - 1;
        if (isFirstOrLastVisible && fab.isOrWillBeShown()) {
            fab.hide();
        } else if (!isFirstOrLastVisible && fab.isOrWillBeHidden()) {
            fab.show();
        }
    }

    private void onFabClicked() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager.findFirstVisibleItemPosition() < 100) {
            recyclerView.smoothScrollToPosition(0);
        } else {
            recyclerView.scrollToPosition(0);
        }
    }
}
