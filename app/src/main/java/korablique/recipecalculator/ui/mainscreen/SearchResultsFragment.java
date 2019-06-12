package korablique.recipecalculator.ui.mainscreen;


import androidx.lifecycle.Lifecycle;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.card.CardDialog;

public class SearchResultsFragment extends BaseFragment {
    public static final String SEARCH_RESULTS_FRAGMENT_TAG = "SEARCH_RESULTS_FRAGMENT_TAG";
    public static final String REQUEST = "REQUEST";
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    Lifecycle lifecycle;
    @Inject
    MainActivity mainActivity;
    @Inject
    FoodstuffsList foodstuffsList;
    @Inject
    RxFragmentSubscriptions fragmentSubscriptions;

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.search_results_layout, container, false);
        String query = getArguments().getString(REQUEST);

        TextView searchRequestTextView = fragmentView.findViewById(R.id.search_results_text_view);
        searchRequestTextView.setText(getResources().getString(R.string.search_results, query));

        RecyclerView searchResultsRecyclerView = fragmentView.findViewById(R.id.search_results_recycler_view);
        SearchResultsAdapter adapter = new SearchResultsAdapter(getActivity(), (foodstuff, position) -> {
            CardDialog cardDialog = CardDialog.showCard(getActivity(), foodstuff);
            cardDialog.prohibitEditing(true);
            cardDialog.setUpAddFoodstuffButton(foodstuff1 -> {
                CardDialog.hideCard(getActivity());
                BucketList bucketList = BucketList.getInstance();
                bucketList.add(foodstuff1);
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            }, R.string.add_foodstuff);
        });
        searchResultsRecyclerView.setAdapter(adapter);

        Single<List<Foodstuff>> searchResultSingle = foodstuffsList.requestFoodstuffsLike(query);
        fragmentSubscriptions.subscribe(searchResultSingle, (searchResult) -> {
            adapter.addItems(searchResult);

            if (adapter.getItemCount() == 0) {
                fragmentView.findViewById(R.id.nothing_found_view).setVisibility(View.VISIBLE);
            } else {
                fragmentView.findViewById(R.id.nothing_found_view).setVisibility(View.GONE);
            }
        });

        return fragmentView;
    }

    public static void show(String request, FragmentActivity context) {
        Fragment searchResultsFragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(REQUEST, request);
        searchResultsFragment.setArguments(args);
        FragmentTransaction transaction = context.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, searchResultsFragment, SEARCH_RESULTS_FRAGMENT_TAG);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
