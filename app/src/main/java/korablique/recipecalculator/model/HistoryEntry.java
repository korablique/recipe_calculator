package korablique.recipecalculator.model;

import java.util.Date;

import korablique.recipecalculator.model.Foodstuff;

public class HistoryEntry {
    private long historyId;
    private WeightedFoodstuff foodstuff;
    private Date time;

    public HistoryEntry(long historyId, WeightedFoodstuff foodstuff, Date time) {
        this.historyId = historyId;
        this.foodstuff = foodstuff;
        this.time = time;
    }

    public long getHistoryId() {
        return historyId;
    }

    public WeightedFoodstuff getFoodstuff() {
        return foodstuff;
    }

    public Date getTime() {
        return time;
    }
}
