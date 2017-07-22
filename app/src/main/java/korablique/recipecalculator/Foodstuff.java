package korablique.recipecalculator;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Foodstuff implements Parcelable, Comparable<Foodstuff> {
    private final long id;
    private final String name;
    private final double weight;
    private final double protein;
    private final double fats;
    private final double carbs;
    private final double calories;

    public Foodstuff(String name, double weight, double protein, double fats, double carbs, double calories) {
        this(-1, name, weight, protein, fats, carbs, calories);
    }

    public Foodstuff(long id, String name, double weight, double protein, double fats, double carbs, double calories) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
        this.calories = calories;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    public double getProtein() {
        return protein;
    }

    public double getFats() {
        return fats;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getCalories() {
        return calories;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeDouble(weight);
        dest.writeDouble(protein);
        dest.writeDouble(fats);
        dest.writeDouble(carbs);
        dest.writeDouble(calories);
    }

    public static final Parcelable.Creator<Foodstuff> CREATOR
            = new Parcelable.Creator<Foodstuff>() {
        public Foodstuff createFromParcel(Parcel in) {
            return new Foodstuff(
                    in.readLong(),
                    in.readString(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readDouble());
        }

        public Foodstuff[] newArray(int size) {
            return new Foodstuff[size];
        }
    };

    @Override
    public int compareTo(@NonNull Foodstuff foodstuff) {
        return name.compareToIgnoreCase(foodstuff.getName());
    }
}
