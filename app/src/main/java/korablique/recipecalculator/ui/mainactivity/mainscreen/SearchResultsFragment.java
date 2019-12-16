package korablique.recipecalculator.ui.mainactivity.mainscreen;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.mainactivity.MainActivity;

public class SearchResultsFragment extends BaseFragment implements ActivityCallbacks.Observer {
    private static final String SEARCH_RESULTS_FRAGMENT_TAG = "SEARCH_RESULTS_FRAGMENT_TAG";
    private static final String INITIAL_QUERY = "INITIAL_QUERY";
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    Lifecycle lifecycle;
    @Inject
    MainActivity mainActivity;
    @Inject
    MainScreenCardController cardController;

    private SearchResultsAdapter adapter;
    @Nullable
    private String query;
    @Nullable
    private List<Foodstuff> searchResults;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity.getActivityCallbacks().addObserver(this);
    }

    @Override
    public void onDestroy() {
        mainActivity.getActivityCallbacks().removeObserver(this);
        super.onDestroy();
    }

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.search_results_layout, container, false);

        // Возможно нам успели сменить query и задать searchResults, пока фрагмент создавал вьюшку
        if (query != null) {
            displayQuery(fragmentView, query);
        } else {
            displayQuery(fragmentView, getArguments().getString(INITIAL_QUERY));
        }
        if (searchResults != null) {
            displaySearchResults(fragmentView, searchResults);
        }

        RecyclerView searchResultsRecyclerView = fragmentView.findViewById(R.id.search_results_recycler_view);
        adapter = new SearchResultsAdapter(getActivity(), (foodstuff, position) -> {
            cardController.showCard(foodstuff);
        });
        searchResultsRecyclerView.setAdapter(adapter);

        Button addNewFoodstuffButton = fragmentView.findViewById(R.id.add_new_foodstuff_button);
        addNewFoodstuffButton.setOnClickListener(v -> {
            EditFoodstuffActivity.startForCreation(this, RequestCodes.MAIN_SCREEN_SEARCH_RESULTS_CREATE_FOODSTUFF);
        });
        return fragmentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.MAIN_SCREEN_SEARCH_RESULTS_CREATE_FOODSTUFF
                && resultCode == Activity.RESULT_OK) {
            Foodstuff foodstuff = data.getParcelableExtra(EditFoodstuffActivity.EXTRA_RESULT_FOODSTUFF);
            cardController.showCard(foodstuff);
        }
    }

    public void setDisplayedQuery(String query) {
        this.query = query;
        View fragmentView = getView();
        if (fragmentView != null) {
            displayQuery(getView(), query);
        }
    }

    private void displayQuery(View fragmentView, String query) {
        TextView searchRequestTextView = fragmentView.findViewById(R.id.search_results_text_view);
        searchRequestTextView.setText(getResources().getString(R.string.search_results, query));
    }

    public void setDisplayedSearchResults(List<Foodstuff> searchResults) {
        this.searchResults = new ArrayList<>(searchResults);
        View fragmentView = getView();
        if (fragmentView != null) {
            displaySearchResults(fragmentView, searchResults);
        }
    }

    private void displaySearchResults(View fragmentView, List<Foodstuff> searchResults) {
        adapter.clear();
        adapter.addItems(searchResults);
        if (adapter.getItemCount() == 0) {
            fragmentView.findViewById(R.id.nothing_found_view).setVisibility(View.VISIBLE);
        } else {
            fragmentView.findViewById(R.id.nothing_found_view).setVisibility(View.GONE);
        }
    }

    public static SearchResultsFragment show(String request, MainScreenFragment context) {
        SearchResultsFragment searchResultsFragment = findFragment(context);
        if (searchResultsFragment != null) {
            return searchResultsFragment;
        }
        FragmentManager fragmentManager = context.getChildFragmentManager();
        searchResultsFragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(INITIAL_QUERY, request);
        searchResultsFragment.setArguments(args);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, searchResultsFragment, SEARCH_RESULTS_FRAGMENT_TAG);
        transaction.commitAllowingStateLoss();
        return searchResultsFragment;
    }

    @Nullable
    public static SearchResultsFragment findFragment(MainScreenFragment context) {
        if (!context.isAdded()) {
            return null;
        }
        FragmentManager fragmentManager = context.getChildFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(SEARCH_RESULTS_FRAGMENT_TAG);
        return (SearchResultsFragment) fragment;
    }

    public static void closeFragment(MainScreenFragment context, SearchResultsFragment fragment) {
        FragmentManager fragmentManager = context.getChildFragmentManager();
        fragmentManager
                .beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss();
    }
}
