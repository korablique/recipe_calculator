package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    private volatile UserParameters cachedUserParameters;

    public interface RequestCurrentUserParametersCallback {
        void onResult(UserParameters userParameters);
    }

    public UserParametersWorker(
            Context context,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        this.context = context;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void initCache() {
        databaseThreadExecutor.execute(() -> updateCache());
    }

    /**
     * Выполняется на том потоке, на котором его вызвали для того,
     * чтобы другой метод мог его вызвать внутри databaseThreadExecutor.execute()
     */
    private void updateCache() {
        requestCurrentUserParametersImpl(context, (userParameters) -> {
            cachedUserParameters = userParameters;
        });
    }

    public void saveUserParameters(
            final Context context, final UserParameters userParameters, final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
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
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }

            updateCache();
        });
    }

    public void requestCurrentUserParameters(
            final Context context,
            final RequestCurrentUserParametersCallback callback) {
        if (cachedUserParameters != null) {
            if (callback != null) {
                callback.onResult(cachedUserParameters);
            }
            return;
        }
        databaseThreadExecutor.execute(() -> {
            requestCurrentUserParametersImpl(context, userParameters -> {
                if (callback != null) {
                    mainThreadExecutor.execute(() -> callback.onResult(userParameters));
                }
            });
        });
    }

    private void requestCurrentUserParametersImpl(
            final Context context,
            final RequestCurrentUserParametersCallback callback) {
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
        callback.onResult(userParameters);
    }
}
