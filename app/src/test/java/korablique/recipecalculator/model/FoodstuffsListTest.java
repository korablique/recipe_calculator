package korablique.recipecalculator.model;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.DatabaseWorker.FinishCallback;
import korablique.recipecalculator.database.DatabaseWorker.FoodstuffsBatchReceiveCallback;
import korablique.recipecalculator.database.FoodstuffsList;

import static korablique.recipecalculator.database.FoodstuffsList.BATCH_SIZE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class FoodstuffsListTest {
    // используется, когда нужно несколько батчей (здесь 2)
    private static final int FOODSTUFFS_NUMBER = 2 * BATCH_SIZE;
    private FoodstuffsList foodstuffsList;
    private DatabaseWorker databaseWorker;
    private Context context;

    private List<Foodstuff> dbFoodstuffs;

    @Before
    public void setUp() {
        databaseWorker = mock(DatabaseWorker.class);
        context = mock(Context.class);
        dbFoodstuffs = new ArrayList<>();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                FoodstuffsBatchReceiveCallback c1 = invocation.getArgument(1);
                FinishCallback c2 = invocation.getArgument(2);
                List<Foodstuff> batch = new ArrayList<>();
                // индексы идут от 1, т.к. 0 % 100 == 0
                for (int index = 1; index <= dbFoodstuffs.size(); index++) {
                    batch.add(dbFoodstuffs.get(index - 1));
                    if (index % BATCH_SIZE == 0) {
                        c1.onReceive(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    c1.onReceive(batch);
                }
                c2.onFinish();
                return null;
            }
        }).when(databaseWorker).requestListedFoodstuffsFromDb(
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        foodstuffsList = new FoodstuffsList(
                databaseWorker, mock(MainThreadExecutor.class), mock(ComputationThreadsExecutor.class));
    }

    // Если клиент вызвал метод, в его коллбек через неопределенное время придут фудстафы.
    @Test
    public void foodstuffsRequestLeadsToCallbackCall() {
        dbFoodstuffs.add(new Foodstuff("a", 1, 2, 3, 4));
        dbFoodstuffs.add(new Foodstuff("b", 4, 3, 2, 1));

        List<Foodstuff> receivedFoodstuffs = new ArrayList<>();
        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {
            receivedFoodstuffs.addAll(foodstuffs);
        });

        Assert.assertEquals(dbFoodstuffs, receivedFoodstuffs);
    }

    // Если клиент вызовет метод первый раз, будет выполнен запрос в БД.
    @Test
    public void firstRequestTouchesDatabase() {
        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {});
        verify(databaseWorker).requestListedFoodstuffsFromDb(
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));
    }

    @Test
    public void foodstuffsReturningInBatches() {
        for (int index = 0; index < FOODSTUFFS_NUMBER; index++) {
            dbFoodstuffs.add(new Foodstuff("a" + index, 1, 2, 3, 4 + index));
        }

        final int[] batchesNumber = {0};
        foodstuffsList.getAllFoodstuffs(
                unused -> {
                    batchesNumber[0] = batchesNumber[0] + 1;
                }, foodstuffs -> {});
        Assert.assertEquals(2, batchesNumber[0]);
    }

    // Если клиент вызовет метод второй раз, он сразу получит все фудстафы (они кешируются).
    @Test
    public void secondRequestDoesNotTouchDatabase() {
        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {});

        reset(databaseWorker);
        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {});
        verifyZeroInteractions(databaseWorker);
    }

    // Если второй клиент вызовет метод во время загрузки данных для первого клиента, то он
    // ждёт, пока будут загружены фудстаффы для первого клиента и затем получит их.
    @Test
    public void ifSecondClientCallMethodDuringLoadingItAwaits() {
        dbFoodstuffs.add(new Foodstuff("a", 1, 2, 3, 4));
        dbFoodstuffs.add(new Foodstuff("b", 4, 3, 2, 1));

        List<FoodstuffsBatchReceiveCallback> batchReceiveCallbacks = new ArrayList<>();
        List<FinishCallback> finishCallbacks = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                FoodstuffsBatchReceiveCallback c1 = invocation.getArgument(1);
                FinishCallback c2 = invocation.getArgument(2);
                batchReceiveCallbacks.add(c1);
                finishCallbacks.add(c2);
                return null;
            }
        }).when(databaseWorker).requestListedFoodstuffsFromDb(
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {});
        List<Foodstuff> receivedFoodstuffs = new ArrayList<>();
        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {
            receivedFoodstuffs.addAll(foodstuffs);
        });
        for (FoodstuffsBatchReceiveCallback batchReceiveCallback : batchReceiveCallbacks) {
            batchReceiveCallback.onReceive(dbFoodstuffs);
        }
        for (FinishCallback finishCallback : finishCallbacks) {
            finishCallback.onFinish();
        }

        // проверяем, что запрос в БД будет только один
        verify(databaseWorker, times(1)).requestListedFoodstuffsFromDb(
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        Assert.assertEquals(dbFoodstuffs, receivedFoodstuffs);
    }

    // Если второй клиент вызовет метод во время загрузки данных для первого клиента, то он
    // получит уже загруженную для первого часть фудстаффов, а затем будет получать остальные батчи
    // через неопределенное время.
    @Test
    public void ifSecondClientCallMethodDuringLoadingItGetAlreadyLoadedFoodstuffsAndThanOtherBatches() {
        for (int index = 0; index < FOODSTUFFS_NUMBER; index++) {
            dbFoodstuffs.add(new Foodstuff("a" + index, 1, 2, 3, 4 + index));
        }
        List<FoodstuffsBatchReceiveCallback> batchReceiveCallbacks = new ArrayList<>();
        List<FinishCallback> finishCallbacks = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                FoodstuffsBatchReceiveCallback c1 = invocation.getArgument(1);
                FinishCallback c2 = invocation.getArgument(2);
                batchReceiveCallbacks.add(c1);
                finishCallbacks.add(c2);
                return null;
            }
        }).when(databaseWorker).requestListedFoodstuffsFromDb(
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        List<Foodstuff> gettingFoodstuffs = new ArrayList<>();

        List<Foodstuff> firstBatchDetector1 = new ArrayList<>();
        List<Foodstuff> secondBatchDetector1 = new ArrayList<>();
        // первый вызов метода
        foodstuffsList.getAllFoodstuffs(batch -> {
                    if (firstBatchDetector1.isEmpty()) {
                        firstBatchDetector1.addAll(batch);
                    } else if (secondBatchDetector1.isEmpty()) {
                        secondBatchDetector1.addAll(batch);
                    } else {
                        throw new AssertionError("Third batch is not expected!");
                    }
                }, foodstuffs -> {});
        // получение первого батча
        List<Foodstuff> firstBatch = new ArrayList<>();
        for (int index = 0; index < BATCH_SIZE; index++) {
            firstBatch.add(dbFoodstuffs.get(index));
        }
        for (FoodstuffsBatchReceiveCallback batchReceiveCallback : batchReceiveCallbacks) {
            batchReceiveCallback.onReceive(firstBatch);
        }
        List<Foodstuff> firstBatchDetector2 = new ArrayList<>();
        List<Foodstuff> secondBatchDetector2 = new ArrayList<>();
        // второй вызов метода
        foodstuffsList.getAllFoodstuffs(batch -> {
                    if (firstBatchDetector2.isEmpty()) {
                        firstBatchDetector2.addAll(batch);
                    } else if (secondBatchDetector2.isEmpty()) {
                        secondBatchDetector2.addAll(batch);
                    } else {
                        throw new AssertionError("Third batch is not expected!");
                    }
                }, foodstuffs -> {
                    gettingFoodstuffs.addAll(foodstuffs);
                });
        // проверяем, что 2-ой клиент сразу получил загруженную часть
        Assert.assertEquals(BATCH_SIZE, firstBatchDetector2.size());
        Assert.assertEquals(firstBatchDetector2, firstBatchDetector1);

        List<Foodstuff> secondBatch = new ArrayList<>(dbFoodstuffs);
        secondBatch.removeAll(firstBatch);
        for (FoodstuffsBatchReceiveCallback batchReceiveCallback : batchReceiveCallbacks) {
            batchReceiveCallback.onReceive(secondBatch);
        }
        Assert.assertFalse(secondBatchDetector2.isEmpty());

        // проверяем, что второй клиент в итоге получил все фудстаффы из БД
        for (FinishCallback finishCallback : finishCallbacks) {
            finishCallback.onFinish();
        }
        Assert.assertEquals(dbFoodstuffs, gettingFoodstuffs);
    }

    @Test
    public void testGetFoodstuffsWithId() {
        dbFoodstuffs.add(Foodstuff.withId(1L).withName("pen").withNutrition(1, 2, 3, 4));
        dbFoodstuffs.add(Foodstuff.withId(2L).withName("apple").withNutrition(1, 2, 3, 4));
        dbFoodstuffs.add(Foodstuff.withId(3L).withName("applepen").withNutrition(1, 2, 3, 4));

        List<Foodstuff> foundFoodstuffs =
                foodstuffsList.getFoodstuffsWithIds(Arrays.asList(1L, 3L)).toList().blockingGet();
        Assert.assertEquals(2, foundFoodstuffs.size());
        Assert.assertEquals(foundFoodstuffs.get(0), dbFoodstuffs.get(0));
        Assert.assertEquals(foundFoodstuffs.get(1), dbFoodstuffs.get(2));
    }

    @Test
    public void testGetsFoodstuffsWithIdInRequestedOreder() {
        dbFoodstuffs.add(Foodstuff.withId(1L).withName("applepen").withNutrition(1, 2, 3, 4));
        dbFoodstuffs.add(Foodstuff.withId(2L).withName("pinapplepen").withNutrition(1, 2, 3, 4));

        // Прямой порядок
        List<Foodstuff> foundFoodstuffs =
                foodstuffsList.getFoodstuffsWithIds(Arrays.asList(1L, 2L)).toList().blockingGet();
        Assert.assertEquals(2, foundFoodstuffs.size());
        Assert.assertEquals(foundFoodstuffs.get(0), dbFoodstuffs.get(0));
        Assert.assertEquals(foundFoodstuffs.get(1), dbFoodstuffs.get(1));

        // Обратный порядок
        foundFoodstuffs =
                foodstuffsList.getFoodstuffsWithIds(Arrays.asList(2L, 1L)).toList().blockingGet();
        Assert.assertEquals(2, foundFoodstuffs.size());
        Assert.assertEquals(foundFoodstuffs.get(0), dbFoodstuffs.get(1));
        Assert.assertEquals(foundFoodstuffs.get(1), dbFoodstuffs.get(0));
    }
}
