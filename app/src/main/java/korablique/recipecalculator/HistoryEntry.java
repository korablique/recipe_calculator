package korablique.recipecalculator;

import java.util.Date;

public class HistoryEntry {
    private long historyId;
    private Foodstuff foodstuff;
    private Date time;

    public HistoryEntry(long historyId, Foodstuff foodstuff, Date time) {
        this.historyId = historyId;
        this.foodstuff = foodstuff;
        this.time = time;
    }

    public long getHistoryId() {
        return historyId;
    }

    public Foodstuff getFoodstuff() {
        return foodstuff;
    }

    public Date getTime() {
        return time;
    }
}
