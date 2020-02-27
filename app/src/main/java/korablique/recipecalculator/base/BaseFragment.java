package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import korablique.recipecalculator.dagger.InjectorHolder;

public abstract class BaseFragment extends Fragment {
    @Inject
    BaseActivity parentActivity;
    private FragmentCallbacks fragmentCallbacks = new FragmentCallbacks();
    private ActivityCallbacks.Observer activityBackListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        InjectorHolder.getInjector().inject(this);
        super.onCreate(savedInstanceState);
        fragmentCallbacks.dispatchFragmentCreate(savedInstanceState);


        activityBackListener = new ActivityCallbacks.Observer() {
            @Override
            public boolean onActivityBackPressed() {
                if (!shouldCloseOnBack()) {
                    return false;
                }
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager == null || !isVisible()) {
                    return false;
                }
                fragmentManager
                        .beginTransaction()
                        .remove(BaseFragment.this)
                        .commit();
                return true;
            }
        };
        parentActivity.getActivityCallbacks().addObserver(activityBackListener);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = createView(inflater, container, savedInstanceState);
        fragmentCallbacks.dispatchFragmentViewCreated(fragmentView, savedInstanceState);
        return fragmentView;
    }

    protected abstract View createView(@NonNull LayoutInflater inflater,
                                       ViewGroup container,
                                       Bundle savedInstanceState);

    protected boolean shouldCloseOnBack() {
        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        fragmentCallbacks.dispatchFragmentSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        fragmentCallbacks.dispatchFragmentRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        fragmentCallbacks.dispatchFragmentStart();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragmentCallbacks.dispatchFragmentDestroy();
        parentActivity.getActivityCallbacks().removeObserver(activityBackListener);
    }

    public FragmentCallbacks getFragmentCallbacks() {
        return fragmentCallbacks;
    }
}
