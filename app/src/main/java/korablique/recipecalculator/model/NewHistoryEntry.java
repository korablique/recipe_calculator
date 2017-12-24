package korablique.recipecalculator.model;

import java.util.Date;

public class NewHistoryEntry {
    private long foodstuffId;
    private double foodstuffWeight;
    private Date date;

    public NewHistoryEntry(long foodstuffId, double foodstuffWeight, Date date) {
        this.foodstuffId = foodstuffId;
        this.foodstuffWeight = foodstuffWeight;
        this.date = date;
    }

    public long getFoodstuffId() {
        return foodstuffId;
    }

    public double getFoodstuffWeight() {
        return foodstuffWeight;
    }

    public Date getDate() {
        return date;
    }
}
