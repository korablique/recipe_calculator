package korablique.recipecalculator.database;

import org.joda.time.DateTime;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import korablique.recipecalculator.base.Function0arg;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.room.UserParametersDao;
import korablique.recipecalculator.database.room.UserParametersEntity;
import korablique.recipecalculator.model.UserParameters;

public class UserParametersWorker {
    private DatabaseHolder databaseHolder;
    private final DatabaseThreadExecutor databaseThreadExecutor;
    private final MainThreadExecutor mainThreadExecutor;
    private volatile Single<Optional<UserParameters>> cachedCurrentUserParameters;
    private volatile Single<Optional<UserParameters>> cachedFirstUserParameters;


    public UserParametersWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        this.databaseHolder = databaseHolder;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void initCache() {
        cachedCurrentUserParameters = requestCurrentUserParametersObservable();
        cachedFirstUserParameters = requestFirstUserParametersObservable();
        // Форсируем моментальный старт ленивого запроса, чтобы запрос в БД фактически стартовал.
        cachedCurrentUserParameters.subscribe();
        cachedFirstUserParameters.subscribe();
    }

    public Single<Optional<UserParameters>> requestCurrentUserParameters() {
        if (cachedCurrentUserParameters == null) {
            cachedCurrentUserParameters = requestCurrentUserParametersObservable();
        }
        return cachedCurrentUserParameters;
    }

    private Single<Optional<UserParameters>> requestCurrentUserParametersObservable() {
        return requestUserParametersByFunction(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            return userDao.loadCurrentUserParameters();
        });
    }

    public Single<Optional<UserParameters>> requestFirstUserParameters() {
        if (cachedFirstUserParameters == null) {
            cachedFirstUserParameters = requestFirstUserParametersObservable();
        }
        return cachedFirstUserParameters;
    }

    private Single<Optional<UserParameters>> requestFirstUserParametersObservable() {
        return requestUserParametersByFunction(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            return userDao.loadFirstUserParameters();
        });
    }

    /**
     * Makes actual request to database.
     * @param function function retrieves concrete user parameters (first/current)
     */
    private Single<Optional<UserParameters>> requestUserParametersByFunction(
            Function0arg<UserParametersEntity> function) {
        Single<Optional<UserParameters>> result = Single.create((subscriber) -> {
            UserParametersEntity entity = function.call();
            if (entity != null) {
                UserParameters params = EntityConverter.toUserParameters(entity);
                subscriber.onSuccess(Optional.of(params));
            } else {
                subscriber.onSuccess(Optional.empty());
            }
        });
        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler())
                .cache();
        return result;
    }

    public Observable<UserParameters> requestAllUserParameters() {
        return requestUserParameters(new DateTime(0), new DateTime(Long.MAX_VALUE));
    }

    public Observable<UserParameters> requestUserParameters(DateTime from, DateTime to) {
        Observable<UserParameters> result = Observable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            List<UserParametersEntity> entities =
                    userDao.loadUserParameters(from.getMillis(), to.getMillis());
            if (entities != null) {
                for (UserParametersEntity entity : entities) {
                    UserParameters userParameters = EntityConverter.toUserParameters(entity);
                    subscriber.onNext(userParameters);
                }
                subscriber.onComplete();
            } else {
                subscriber.onComplete();
            }
        });
        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler())
                .cache();
        return result;
    }

    public Completable saveUserParameters(
            final UserParameters userParameters) {
        Completable result = Completable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            UserParametersEntity userParametersEntity = EntityConverter.toEntity(userParameters);
            long insertedParamsId = userDao.insertUserParameters(userParametersEntity);

            if (insertedParamsId < 0) {
                subscriber.onError(new IllegalStateException("Could not insert user parameters"));
            }

            subscriber.onComplete();
        });

        result = result
                .subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler())
                .cache();

        // Сразу подписываемся на получивщийся Completable, чтобы форсировать немедленный
        // старт операции без ожидания подписчиков (кажется естественным, что клиенты могут вызывать
        // saveUserParameters и не особо интересоваться моментом, когда сохранение будет завершено,
        // т.е. вообще не подписываться на Completable).
        result.subscribe();

        // Мы вставили новые параметры пользователя в БД, нужно не забыть
        // обновить закешированное значение.
        cachedCurrentUserParameters = Single.just(Optional.of(userParameters));

        // для тестов, т.к. там не вызывается initCache()
        if (cachedFirstUserParameters == null) {
            cachedFirstUserParameters = cachedCurrentUserParameters;
        }

        cachedFirstUserParameters = cachedFirstUserParameters
            .flatMap((cachedFirstVal) -> {
                if (cachedFirstVal.isPresent()) {
                    return Single.just(cachedFirstVal);
                } else {
                    // если первых параметров нет - значит, они ещё не успели сохраниться,
                    // поэтому возвращаем сохраняемые (т.к. они и есть первые)
                    return Single.just(Optional.of(userParameters));
                }
            });

        return result;
    }
}
