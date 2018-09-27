package korablique.recipecalculator.ui.bucketlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.model.WeightedFoodstuff;

public class BucketList {
    private static BucketList instance;
    private List<WeightedFoodstuff> bucketList = new ArrayList<>();

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
        bucketList.add(wf);
    }

    public void remove(WeightedFoodstuff wf) {
        bucketList.remove(wf);
    }

    public void clear() {
        bucketList.clear();
    }
}
