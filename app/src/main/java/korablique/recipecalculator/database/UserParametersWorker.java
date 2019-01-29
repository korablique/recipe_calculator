package korablique.recipecalculator.database;

import androidx.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.room.UserParametersDao;
import korablique.recipecalculator.database.room.UserParametersEntity;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.UserParameters;

public class UserParametersWorker {
    private DatabaseHolder databaseHolder;
    private final DatabaseThreadExecutor databaseThreadExecutor;
    private final MainThreadExecutor mainThreadExecutor;
    private volatile Single<Optional<UserParameters>> cachedUserParameters;

    public UserParametersWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        this.databaseHolder = databaseHolder;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void initCache() {
        cachedUserParameters = requestCurrentUserParametersObservable();
        // Форсируем моментальный старт ленивого запроса, чтобы запрос в БД фактически стартовал.
        cachedUserParameters.subscribe();
    }

    public Single<Optional<UserParameters>> requestCurrentUserParameters() {
        if (cachedUserParameters == null) {
            cachedUserParameters = requestCurrentUserParametersObservable();
        }
        return cachedUserParameters;
    }

    /**
     * Преобразует вызов {@link #requestCurrentUserParametersImpl}, который должен быть
     * сделан на потоке БД, в создание Observable.
     */
    private Single<Optional<UserParameters>> requestCurrentUserParametersObservable() {
        Single<Optional<UserParameters>> result = Single.create((subscriber) -> {
            UserParameters params = requestCurrentUserParametersImpl();
            if (params != null) {
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

    /**
     * Делает фактический запрос к БД, должен быть вызван на потоке БД.
     */
    @Nullable
    private UserParameters requestCurrentUserParametersImpl() {
        AppDatabase database = databaseHolder.getDatabase();
        UserParametersDao userDao = database.userParametersDao();
        UserParametersEntity userEntity = userDao.loadCurrentUserParameters();

        UserParameters userParameters = null;
        if (userEntity != null) {
            int goalId = userEntity.getGoalId();
            Goal goal = Goal.fromId(goalId);

            int genderId = userEntity.getGenderId();
            Gender gender = Gender.fromId(genderId);

            int age = userEntity.getAge();
            int height = userEntity.getHeight();
            int weight = userEntity.getWeight();

            int lifestyleId = userEntity.getLifestyleId();
            Lifestyle lifestyle = Lifestyle.fromId(lifestyleId);

            int formulaId = userEntity.getFormulaId();
            Formula formula = Formula.fromId(formulaId);

            // TODO: 25.01.19 после изменений в БД получить targetWeight из БД
            int targetWeight;
            if (goal == Goal.LOSING_WEIGHT) {
                targetWeight = weight - 1;
            } else if (goal == Goal.MAINTAINING_CURRENT_WEIGHT) {
                targetWeight = weight;
            } else {
                targetWeight = weight + 1;
            }
            userParameters = new UserParameters(targetWeight, gender, age, height, weight, lifestyle, formula);
        }
        return userParameters;
    }

    public Completable saveUserParameters(
            final UserParameters userParameters) {
        Completable result = Completable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            UserParametersEntity userParametersEntity = new UserParametersEntity(
                    userParameters.getGoal().getId(),
                    userParameters.getGender().getId(),
                    userParameters.getAge(),
                    userParameters.getHeight(),
                    userParameters.getWeight(),
                    userParameters.getLifestyle().getId(),
                    userParameters.getFormula().getId());
            long insertedParamsId = userDao.insertUserParameters(userParametersEntity);

            if (insertedParamsId < 0) {
                subscriber.onError(new IllegalStateException("Could not insert user parameters"));
            }

            // Мы вставили новые параметры пользователя в БД, нужно не забыть
            // обновить закешированное значение.
            cachedUserParameters = Single.just(Optional.of(userParameters));
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

        return result;
    }
}
