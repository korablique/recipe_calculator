package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.UserParameters;

import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_AGE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_LIFESTYLE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GOAL;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

public class UserParametersWorker {
    private final Context context;
    private final DatabaseThreadExecutor databaseThreadExecutor;
    private final MainThreadExecutor mainThreadExecutor;
    private volatile Single<Optional<UserParameters>> cachedUserParameters;

    public UserParametersWorker(
            Context context,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        this.context = context;
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
        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = database.query(
                USER_PARAMETERS_TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                UserParametersContract.ID + " DESC",
                String.valueOf(1));
        UserParameters userParameters = null;
        if (cursor.moveToNext()) {
            int goalId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_GOAL));
            Goal goal = Goal.fromId(goalId);

            int genderId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_GENDER));
            Gender gender = Gender.fromId(genderId);

            int age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
            int height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
            int weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));

            int lifestyleId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_LIFESTYLE));
            Lifestyle lifestyle = Lifestyle.fromId(lifestyleId);

            int formulaId = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
            Formula formula = Formula.fromId(formulaId);

            userParameters = new UserParameters(goal, gender, age, height, weight, lifestyle, formula);
        }
        cursor.close();
        return userParameters;
    }

    public Completable saveUserParameters(
            final UserParameters userParameters) {
        Completable result = Completable.create((subscriber) -> {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_GOAL, userParameters.getGoal().getId());
            values.put(COLUMN_NAME_GENDER, userParameters.getGender().getId());
            values.put(COLUMN_NAME_AGE, userParameters.getAge());
            values.put(COLUMN_NAME_HEIGHT, userParameters.getHeight());
            values.put(COLUMN_NAME_USER_WEIGHT, userParameters.getWeight());
            values.put(COLUMN_NAME_LIFESTYLE, userParameters.getLifestyle().getId());
            values.put(COLUMN_NAME_FORMULA, userParameters.getFormula().getId());
            database.insert(USER_PARAMETERS_TABLE_NAME, null, values);

            // Мы вставили новые параметры пользователя в БД, нужно не забыть
            // обновить закешированное значение.
            cachedUserParameters = Single.just(Optional.of(userParameters));
            subscriber.onComplete();
        });

        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
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
