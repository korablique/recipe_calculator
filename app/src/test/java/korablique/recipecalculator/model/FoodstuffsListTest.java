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
import java.util.List;

import korablique.recipecalculator.BuildConfig;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.DatabaseWorker.FinishCallback;
import korablique.recipecalculator.database.DatabaseWorker.FoodstuffsBatchReceiveCallback;
import korablique.recipecalculator.database.HistoryWorker;

import static korablique.recipecalculator.model.FoodstuffsList.BATCH_SIZE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class FoodstuffsListTest {
    private FoodstuffsList foodstuffsList;
    private DatabaseWorker databaseWorker;
    private HistoryWorker historyWorker;
    private Context context;

    private List<Foodstuff> dbFoodstuffs;

    @Before
    public void setUp() {
        databaseWorker = mock(DatabaseWorker.class);
        historyWorker = mock(HistoryWorker.class);
        context = mock(Context.class);
        dbFoodstuffs = new ArrayList<>();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                FoodstuffsBatchReceiveCallback c1 = invocation.getArgument(2);
                FinishCallback c2 = invocation.getArgument(3);
                List<Foodstuff> batch = new ArrayList<>();
                // индексы идут от 1, т.к. 0 % 100 == 0
                for (int index = 1; index <= dbFoodstuffs.size(); index++) {
                    batch.add(dbFoodstuffs.get(index - 1));
                    if (index % 100 == 0) {
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
                any(Context.class),
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        foodstuffsList = new FoodstuffsList(context, databaseWorker, historyWorker);
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
                any(Context.class),
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));
    }

    @Test
    public void foodstuffsReturningInBatches() {
        // добавляем 200 фудстаффов, чтобы батча было 2
        for (int index = 0; index < 200; index++) {
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
                FoodstuffsBatchReceiveCallback c1 = invocation.getArgument(2);
                FinishCallback c2 = invocation.getArgument(3);
                batchReceiveCallbacks.add(c1);
                finishCallbacks.add(c2);
                return null;
            }
        }).when(databaseWorker).requestListedFoodstuffsFromDb(
                any(Context.class),
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {});
        List<Foodstuff> receivedFoodstuffs = new ArrayList<>();
        foodstuffsList.getAllFoodstuffs(unused -> {}, foodstuffs -> {
            receivedFoodstuffs.addAll(foodstuffs);
        });
        // TODO: 07.11.18 здесь надо вызывать этот коллбэк второй раз?
        for (FoodstuffsBatchReceiveCallback batchReceiveCallback : batchReceiveCallbacks) {
            batchReceiveCallback.onReceive(dbFoodstuffs);
        }
        for (FinishCallback finishCallback : finishCallbacks) {
            finishCallback.onFinish();
        }

        // проверяем, что запрос в БД будет только один
        verify(databaseWorker, times(1)).requestListedFoodstuffsFromDb(
                any(Context.class),
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
        // добавляем 200 фудстаффов, чтобы батча было 2
        for (int index = 0; index < 200; index++) {
            dbFoodstuffs.add(new Foodstuff("a" + index, 1, 2, 3, 4 + index));
        }
        List<FoodstuffsBatchReceiveCallback> batchReceiveCallbacks = new ArrayList<>();
        List<FinishCallback> finishCallbacks = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                FoodstuffsBatchReceiveCallback c1 = invocation.getArgument(2);
                FinishCallback c2 = invocation.getArgument(3);
                batchReceiveCallbacks.add(c1);
                finishCallbacks.add(c2);
                return null;
            }
        }).when(databaseWorker).requestListedFoodstuffsFromDb(
                any(Context.class),
                anyInt(),
                any(FoodstuffsBatchReceiveCallback.class),
                any(FinishCallback.class));

        List<Foodstuff> gettingFoodstuffs = new ArrayList<>();

        // первый вызов метода
        List<Foodstuff> firstBatchForFirstClient = new ArrayList<>();
        foodstuffsList.getAllFoodstuffs(batch -> {
                    firstBatchForFirstClient.addAll(batch);
                }, foodstuffs -> {});
        // получение первого батча
        List<Foodstuff> firstBatch = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            firstBatch.add(dbFoodstuffs.get(index));
        }
        for (FoodstuffsBatchReceiveCallback batchReceiveCallback : batchReceiveCallbacks) {
            batchReceiveCallback.onReceive(firstBatch);
        }
        // второй вызов метода
        List<Foodstuff> firstBatchForSecondClient = new ArrayList<>();
        foodstuffsList.getAllFoodstuffs(batch -> {
                    firstBatchForSecondClient.addAll(batch);
                }, foodstuffs -> {
                    gettingFoodstuffs.addAll(foodstuffs);
                });
        // проверяем, что 2-ой клиент сразу получил загруженную часть
        Assert.assertEquals(BATCH_SIZE, firstBatchForSecondClient.size());
        Assert.assertEquals(firstBatchForSecondClient, firstBatchForFirstClient);

        List<Foodstuff> secondBatch = new ArrayList<>(dbFoodstuffs);
        secondBatch.removeAll(firstBatch);
        for (FoodstuffsBatchReceiveCallback batchReceiveCallback : batchReceiveCallbacks) {
            batchReceiveCallback.onReceive(secondBatch);
        }
        // TODO: 08.11.18 проверить что пришел второй батч

        // проверяем, что второй клиент в итоге получил все фудстаффы из БД
        for (FinishCallback finishCallback : finishCallbacks) {
            finishCallback.onFinish();
        }
        Assert.assertEquals(dbFoodstuffs, gettingFoodstuffs);
    }
}
