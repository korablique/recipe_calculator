package korablique.recipecalculator.database;

import android.content.Context;
import android.util.MutableBoolean;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import io.reactivex.Completable;
import io.reactivex.Single;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.InstantMainThreadExecutor;
import korablique.recipecalculator.util.TestingTimeProvider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserParametersWorkerTest {
    private Context context;
    private DatabaseHolder databaseHolder;
    private UserParametersWorker userParametersWorker;
    private DatabaseThreadExecutor spiedDatabaseThreadExecutor;
    private TimeProvider timeProvider;

    @Before
    public void setUp() throws IOException {
        context = InstrumentationRegistry.getTargetContext();
        spiedDatabaseThreadExecutor = spy(new InstantDatabaseThreadExecutor());
        databaseHolder = new DatabaseHolder(context, spiedDatabaseThreadExecutor);
        timeProvider = new TestingTimeProvider();

        databaseHolder.getDatabase().clearAllTables();

        userParametersWorker =
                new UserParametersWorker(
                        databaseHolder, new InstantMainThreadExecutor(), spiedDatabaseThreadExecutor);
    }

    @Test
    public void canSaveAndRetrieveUserParameters() {
        UserParameters userParameters = new UserParameters(
                60,
                Gender.MALE,
                new LocalDate(1993, 7, 20),
                165,
                64,
                Lifestyle.INSIGNIFICANT_ACTIVITY,
                Formula.HARRIS_BENEDICT,
                timeProvider.nowUtc().getMillis());

        MutableBoolean saved = new MutableBoolean(false);
        Completable callback = userParametersWorker.saveUserParameters(userParameters);
        callback.subscribe(() -> {
            saved.value = true;
        });

        Assert.assertTrue(saved.value);

        Single<Optional<UserParameters>> retrievedParamsObservable =
                userParametersWorker.requestCurrentUserParameters();
        UserParameters[] retrievedParams = new UserParameters[1];
        retrievedParamsObservable.subscribe((params) -> {
            retrievedParams[0] = params.get();
        });
        Assert.assertEquals(userParameters, retrievedParams[0]);
    }

    @Test
    public void whenHasCache_UserParamsWorkerDoesNotUseDatabase()  {
        // сохраняем в БД параметры пользователя
        userParametersWorker.initCache();
        UserParameters userParameters = new UserParameters(
                60,
                Gender.MALE,
                new LocalDate(1993, 7, 20),
                165,
                64,
                Lifestyle.INSIGNIFICANT_ACTIVITY,
                Formula.HARRIS_BENEDICT,
                timeProvider.nowUtc().getMillis());
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

    @Test
    public void requestFirstUserParametersWorksCorrectly() {
        LocalDate dateOfBirth = new LocalDate(1989, 10, 10);
        DateTime date1 = new DateTime(2019, 1, 1, 12, 0, DateTimeZone.UTC);
        UserParameters userParameters1 = new UserParameters(50, Gender.FEMALE, dateOfBirth,
                160, 60, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, date1.getMillis());
        DateTime date2 = new DateTime(2019, 2, 1, 12, 0, DateTimeZone.UTC);
        UserParameters userParameters2 = new UserParameters(50, Gender.FEMALE, dateOfBirth,
                160, 59, Lifestyle.INSIGNIFICANT_ACTIVITY, Formula.HARRIS_BENEDICT, date2.getMillis());
        DateTime date3 = new DateTime(2019, 3, 1, 12, 0, DateTimeZone.UTC);
        UserParameters userParameters3 = new UserParameters(50, Gender.FEMALE, dateOfBirth,
                160, 58, Lifestyle.INSIGNIFICANT_ACTIVITY, Formula.MIFFLIN_JEOR, date3.getMillis());
        userParametersWorker.saveUserParameters(userParameters1);
        userParametersWorker.saveUserParameters(userParameters2);
        userParametersWorker.saveUserParameters(userParameters3);
        Single<Optional<UserParameters>> firstParamsSingle = userParametersWorker.requestFirstUserParameters();

        UserParameters[] retrievedParams = new UserParameters[1];
        firstParamsSingle.subscribe((params) -> {
            retrievedParams[0] = params.get();
        });
        Assert.assertEquals(userParameters1, retrievedParams[0]);
    }
}
