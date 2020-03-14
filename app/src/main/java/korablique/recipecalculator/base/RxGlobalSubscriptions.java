package korablique.recipecalculator.base;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.Subject;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.model.HistoryEntry;

/**
 * В целом то же, что {@link RxActivitySubscriptions} и {@link RxFragmentSubscriptions},
 * но просто делает вид, что что-то делает с Disposable - глобальный scope "заканчивается"
 * только со смертью процесса, поэтому принимая Disposable класс просто их игнорирует
 */
@Singleton
public class RxGlobalSubscriptions implements ActivityCallbacks.Observer {
    @Inject
    public RxGlobalSubscriptions() {
    }

    public void storeDisposable(Disposable disposable) {
    }

    public void add(Disposable disposable) {
    }
}
