package korablique.recipecalculator.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.MutableBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.ref.Reference;

import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.util.DbUtil;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.InstantMainThreadExecutor;

import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserParametersWorkerTest {
    private Context context;
    private UserParametersWorker userParametersWorker;
    private DatabaseThreadExecutor spiedDatabaseThreadExecutor;

    @Before
    public void setUp() throws IOException {
        context = InstrumentationRegistry.getTargetContext();

        FoodstuffsDbHelper.deinitializeDatabase(context);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
        dbHelper.initializeDatabase();

        DbUtil.clearTable(context, HISTORY_TABLE_NAME);
        DbUtil.clearTable(context, FOODSTUFFS_TABLE_NAME);

        spiedDatabaseThreadExecutor = spy(new InstantDatabaseThreadExecutor());

        userParametersWorker =
                new UserParametersWorker(
                        context, new InstantMainThreadExecutor(), spiedDatabaseThreadExecutor);
    }

    @Test
    public void canSaveAndRetrieveUserParameters() {
        UserParameters userParameters = new UserParameters(
                context.getResources().getStringArray(R.array.goals_array)[0],
                context.getResources().getStringArray(R.array.gender_array)[0],
                24,
                165,
                64,
                1.375f,
                context.getResources().getStringArray(R.array.formula_array)[0]);

        MutableBoolean saved = new MutableBoolean(false);
        Completable callback = userParametersWorker.saveUserParameters(userParameters);
        callback.subscribe(() -> {
            saved.value = true;
        });

        Assert.assertTrue(saved.value);

        Single<Optional<UserParameters>> retrievedParamsObservable =
                userParametersWorker.requestCurrentUserParameters();
        UserParameters[] retreivedParams = new UserParameters[1];
        retrievedParamsObservable.subscribe((params) -> {
            retreivedParams[0] = params.get();
        });
        Assert.assertEquals(userParameters, retreivedParams[0]);
    }

    @Test
    public void whenHasCache_UserParamsWorkerDoesNotUseDatabase()  {
        // сохраняем в БД параметры пользователя
        userParametersWorker.initCache();
        UserParameters userParameters = new UserParameters(
                context.getResources().getStringArray(R.array.goals_array)[0],
                context.getResources().getStringArray(R.array.gender_array)[0],
                24,
                165,
                64,
                1.375f,
                context.getResources().getStringArray(R.array.formula_array)[0]);
        userParametersWorker.saveUserParameters(userParameters);

        reset(spiedDatabaseThreadExecutor);
        userParametersWorker.requestCurrentUserParameters();
        // проверяем, что databaseThreadExecutor не делал запрос в БД, т к есть кеш
        verify(spiedDatabaseThreadExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    public void whenHasNoCache_UserParamsWorkerUsesDatabase() {
        // Note: мы НЕ делаем userParams.initCache(),
        // т.к. проверяем, что при отсутствии кеша UserParamsWorker будет взаимодействовать с БД
        reset(spiedDatabaseThreadExecutor);

        userParametersWorker.requestCurrentUserParameters();
        // Через шпиона убеждаемся, что UserParamsWorker взаимодействовал с БД,
        // т.к. у него 100% отсутствовал кеш и достать параметры юзера он больше ни откуда не мог
        verify(spiedDatabaseThreadExecutor).asScheduler();
    }
}
