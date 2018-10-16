package korablique.recipecalculator.ui.mainscreen;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.card.CardDialog;

public class SearchResultsFragment extends Fragment {
    public static final String SEARCH_RESULTS = "SEARCH_RESULTS";
    public static final String REQUEST = "REQUEST";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.search_results_layout, container, false);
        String request = getArguments().getString(REQUEST);

        TextView searchRequestTextView = fragmentView.findViewById(R.id.search_results_text_view);
        searchRequestTextView.setText(getResources().getString(R.string.search_results, request));

        RecyclerView searchResultsRecyclerView = fragmentView.findViewById(R.id.search_results_recycler_view);
        SearchResultsAdapter adapter = new SearchResultsAdapter((foodstuff, position) -> {
            CardDialog cardDialog = CardDialog.showCard(getActivity(), foodstuff);
            cardDialog.prohibitEditing(true);
            cardDialog.setOnAddFoodstuffButtonClickListener(foodstuff1 -> {
                CardDialog.hideCard(getActivity());
                BucketList bucketList = BucketList.getInstance();
                bucketList.add(foodstuff1);
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            });
        });
        searchResultsRecyclerView.setAdapter(adapter);
        List<Foodstuff> searchResults = getArguments().getParcelableArrayList(SEARCH_RESULTS);
        adapter.addItems(searchResults);

        return fragmentView;
    }

    public static void show(String request, List<Foodstuff> searchResults, FragmentActivity context) {
        Fragment searchResultsFragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(REQUEST, request);
        args.putParcelableArrayList(SEARCH_RESULTS, new ArrayList<>(searchResults));
        searchResultsFragment.setArguments(args);
        FragmentTransaction transaction = context.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, searchResultsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
