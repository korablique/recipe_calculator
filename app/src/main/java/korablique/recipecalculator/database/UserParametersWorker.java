package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.support.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.model.UserParameters;

import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_AGE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_COEFFICIENT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GOAL;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

public class UserParametersWorker {
    private Context context;
    private DatabaseThreadExecutor databaseThreadExecutor;
    private MainThreadExecutor mainThreadExecutor;
    private Single<Optional<UserParameters>> cachedUserParameters;

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
        // Форсируем моментальный старт иначе ленивого запроса, чтобы запрос в БД фактически стартовал.
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
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
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
            String goal = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GOAL));
            String gender = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GENDER));
            int age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
            int height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
            int weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));
            float coefficient = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_COEFFICIENT));
            String formula = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
            userParameters = new UserParameters(goal, gender, age, height, weight, coefficient, formula);
        }
        cursor.close();
        return userParameters;
    }

    public Completable saveUserParameters(
            final UserParameters userParameters) {
        Completable result = Completable.create((subscriber) -> {
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_GOAL, userParameters.getGoal());
            values.put(COLUMN_NAME_GENDER, userParameters.getGender());
            values.put(COLUMN_NAME_AGE, userParameters.getAge());
            values.put(COLUMN_NAME_HEIGHT, userParameters.getHeight());
            values.put(COLUMN_NAME_USER_WEIGHT, userParameters.getWeight());
            values.put(COLUMN_NAME_COEFFICIENT, userParameters.getPhysicalActivityCoefficient());
            values.put(COLUMN_NAME_FORMULA, userParameters.getFormula());
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
        // saveUserParameters и не особо интересоваться моментом, когда сохранение будет завершено).
        result.subscribe();

        return result;
    }
}
