package korablique.recipecalculator.base;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import korablique.recipecalculator.dagger.FragmentScope;

/**
 * Управляет подписками фрагмента на Observable'ы
 */
@FragmentScope
public class RxFragmentSubscriptions implements FragmentCallbacks.Observer {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public RxFragmentSubscriptions(FragmentCallbacks fragmentCallbacks) {
        fragmentCallbacks.addObserver(this);
    }

    public <T> void subscribe(Single<T> obs, Consumer<T> consumer) {
        Disposable disposable = obs.subscribe(consumer);
        compositeDisposable.add(disposable);
    }

    public void subscribe(Completable obs, Action action) {
        Disposable disposable = obs.subscribe(action);
        compositeDisposable.add(disposable);
    }

    @Override
    public void onFragmentDestroy() {
        compositeDisposable.dispose();
    }
}
