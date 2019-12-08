package korablique.recipecalculator.base;

import javax.inject.Inject;

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
 * Все методы {@code subscribe} в Rx возвращают объекты класса {@link Disposable} (например,
 * метод {@link Observable#subscribe()}). Объекты типа {@link Disposable} представляют исполняемый
 * (сейчас или в ближайшем будущем) запрос, сделанный вызовом метода {@code subscribe}.
 * <br>
 * "Disposable" в данном случае означает "отменяемый", метод {@link Disposable#dispose()} нужно
 * вызывать как только сделанный методом {@code subscribe} запрос больше не нужен.
 * <br>
 * Например, если пользователь открывает экран Истории, мы сделаем запрос в БД чтобы отобразить
 * пользователю его историю, но если пользователб затем немедленно закроет экран Истории - запрос
 * в БД окажется больше не нужен, и мы могли бы без вреда его отменить.
 * <br>
 * Метод {@link Disposable#dispose()} делает именно это - он отменяет запрос, который представляет.
 * Метод запроса Истории (например, {@link HistoryWorker#requestAllHistoryFromDb}, или похожий)
 * мог бы вернуть объект {@link Observable<HistoryEntry>}, чей метод {@link Observable#subscribe()}
 * будет возвращать {@link Disposable} - если мы затем вызовем метод {@link Disposable#dispose()}
 * прежде чем запрос в БД на самом деле стартует, мы фактически отменим запрос к БД.
 * <br>
 * Всё написанное выше для нас значит, что как только текущая Activity начинает уничтожаться
 * (система вызывает {@link BaseActivity#onDestroy()}), нам нужно вызвать {@link Disposable#dispose()}
 * на всех объектах Disposable, представляющих запросы нужные текущей Activity.
 * <br>
 * Не хотелось бы заставлять всех наследников {@link BaseActivity} хранить все объекты Disposable и
 * вызывать у них dispose внутри onDestroy, потому что это создаст очень много дублирования кода.
 * Вместо этого, мы используем класс {@link RxActivitySubscriptions}, который живёт одновремнно
 * с Activity (потому что является ActivityScoped-синглтоном), сам вызывает все методы {@code subscribe},
 * сохраняет полученные объекты Disposable и вызывает у них dispose, как только получает сигнал
 * об уничтожении своей Activity.
 */
// короче управляет подписками на Observable'ы
@ActivityScope
public class RxActivitySubscriptions implements ActivityCallbacks.Observer {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public RxActivitySubscriptions(ActivityCallbacks activityCallbacks) {
        activityCallbacks.addObserver(this);
    }

    public <T> void subscribe(Single<T> obs, Consumer<T> consumer) {
        Disposable disposable = obs.subscribe(consumer);
        compositeDisposable.add(disposable);
    }

    public <T> void subscribe(Subject<T> obs, Consumer<T> consumer) {
        Disposable disposable = obs.subscribe(consumer);
        compositeDisposable.add(disposable);
    }

    public void subscribe(Completable obs) {
        subscribe(obs, ()->{});
    }

    public void subscribe(Completable obs, Action action) {
        Disposable disposable = obs.subscribe(action);
        compositeDisposable.add(disposable);
    }

    public void storeDisposable(Disposable disposable) {
        compositeDisposable.add(disposable);
    }

    @Override
    public void onActivityDestroy() {
        compositeDisposable.dispose();
    }
}
