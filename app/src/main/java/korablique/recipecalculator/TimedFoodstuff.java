package korablique.recipecalculator;

import java.util.Date;

public class TimedFoodstuff {
    private long historyId;
    private Foodstuff foodstuff;
    private Date time;
    private double weight;

    public TimedFoodstuff(long historyId, Foodstuff foodstuff, Date time, double weight) {
        this.historyId = historyId;
        this.foodstuff = foodstuff;
        this.time = time;
        this.weight = weight;
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

    public double getWeight() {
        return weight;
    }
}
