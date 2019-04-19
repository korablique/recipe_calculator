package korablique.recipecalculator.ui.bucketlist;

import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.WrongThreadException;
import korablique.recipecalculator.model.WeightedFoodstuff;

public class BucketList {
    public interface Observer {
        void onFoodstuffAdded(WeightedFoodstuff wf);
    }
    private static BucketList instance;
    private List<WeightedFoodstuff> bucketList = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();

    private BucketList() {}

    public static synchronized BucketList getInstance() {
        if (instance == null) {
            instance = new BucketList();
        }
        return instance;
    }

    public List<WeightedFoodstuff> getList() {
        return Collections.unmodifiableList(bucketList);
    }

    public void add(WeightedFoodstuff wf) {
        checkCurrentThread();
        bucketList.add(wf);
        for (Observer observer : observers) {
            observer.onFoodstuffAdded(wf);
        }
    }

    public void remove(WeightedFoodstuff wf) {
        checkCurrentThread();
        bucketList.remove(wf);
    }

    public void clear() {
        checkCurrentThread();
        bucketList.clear();
    }

    public void addObserver(Observer o) {
        checkCurrentThread();
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        checkCurrentThread();
        observers.remove(o);
    }

    private void checkCurrentThread() {
        if (Thread.currentThread().getId() != Looper.getMainLooper().getThread().getId()) {
            throw new WrongThreadException("Can't invoke BucketList's methods from not UI thread");
        }
    }
}
