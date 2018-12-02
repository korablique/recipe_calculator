package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import korablique.recipecalculator.dagger.InjectorHolder;

public abstract class BaseFragment extends Fragment {
    private FragmentCallbacks fragmentCallbacks = new FragmentCallbacks();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InjectorHolder.getInjector().inject(this);
        fragmentCallbacks.dispatchFragmentCreate(savedInstanceState);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = createView(inflater, container, savedInstanceState);
        fragmentCallbacks.dispatchFragmentViewCreated(fragmentView);
        return fragmentView;
    }

    protected abstract View createView(@NonNull LayoutInflater inflater,
                                       ViewGroup container,
                                       Bundle savedInstanceState);

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        fragmentCallbacks.dispatchFragmentSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        fragmentCallbacks.dispatchFragmentRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentCallbacks.dispatchFragmentResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fragmentCallbacks.dispatchActivityResult(requestCode, resultCode, data);
    }

    public FragmentCallbacks getFragmentCallbacks() {
        return fragmentCallbacks;
    }
}
