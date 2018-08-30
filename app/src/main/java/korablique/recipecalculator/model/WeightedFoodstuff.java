package korablique.recipecalculator.model;

import android.os.Parcel;
import android.os.Parcelable;

import korablique.recipecalculator.FloatUtils;

public class WeightedFoodstuff implements Parcelable {
    private final Foodstuff foodstuff;
    private double weight;

    WeightedFoodstuff(Foodstuff foodstuff, double weight) {
        this.foodstuff = foodstuff;
        this.weight = weight;
    }

    protected WeightedFoodstuff(Parcel in) {
        foodstuff = in.readParcelable(Foodstuff.class.getClassLoader());
        weight = in.readDouble();
    }

    public long getId() {
        return foodstuff.getId();
    }

    public String getName() {
        return foodstuff.getName();
    }

    public double getProtein() {
        return foodstuff.getProtein();
    }

    public double getFats() {
        return foodstuff.getFats();
    }

    public double getCarbs() {
        return foodstuff.getCarbs();
    }

    public double getCalories() {
        return foodstuff.getCalories();
    }

    public double getWeight() {
        return weight;
    }

    public Foodstuff withoutWeight() {
        return foodstuff;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WeightedFoodstuff) {
            WeightedFoodstuff other = (WeightedFoodstuff) o;
            return foodstuff.equals(other.foodstuff)
                    && FloatUtils.areFloatsEquals(weight, other.weight);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return foodstuff.hashCode();
    }

    public static final Parcelable.Creator<WeightedFoodstuff> CREATOR
            = new Parcelable.Creator<WeightedFoodstuff>() {
        public WeightedFoodstuff createFromParcel(Parcel in) {
            Foodstuff foodstuff = in.readParcelable(Foodstuff.class.getClassLoader());
            double weight = in.readDouble();
            return new WeightedFoodstuff(foodstuff, weight);
        }

        public WeightedFoodstuff[] newArray(int size) {
            return new WeightedFoodstuff[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(foodstuff, flags);
        dest.writeDouble(weight);
    }
}
