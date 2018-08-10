package korablique.recipecalculator.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import korablique.recipecalculator.FloatUtils;

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

    /**
     * Копирует фудстафф в новый объект, но с айдишником.
     * Если передаётся фудстафф, который уже имеет id, это значит, что мы пытаемся изменить id,
     * что запрещено (приводит к выбросу исключения IllegalArgumentException).
     */
    public Foodstuff(long id, Foodstuff foodstuff) {
        if (foodstuff.getId() != -1) {
            throw new IllegalArgumentException("Изменение id запрещено.");
        }
        this.id = id;
        this.name = foodstuff.name;
        this.weight = foodstuff.weight;
        this.protein = foodstuff.protein;
        this.fats = foodstuff.fats;
        this.carbs = foodstuff.carbs;
        this.calories = foodstuff.calories;
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Foodstuff) {
            Foodstuff foodstuff = (Foodstuff) o;
            return id == foodstuff.id && name.equals(foodstuff.name) && haveSameNutrition(this, foodstuff);
        } else {
            // если передан аргумент неправильного типа, метод обязан возвращать false,
            // или если передан null (в этом случае данная проверка всё равно сработает)
            return false;
        }
    }

    /**
     * @param lhs - left hand side
     * @param rhs - right hand side
     * @return true, if foodstuffs' nutritions are equal
     */
    public static boolean haveSameNutrition(Foodstuff lhs, Foodstuff rhs) {
        if (FloatUtils.areFloatsEquals(lhs.getProtein(), rhs.getProtein())
                && FloatUtils.areFloatsEquals(lhs.getFats(), rhs.getFats())
                && FloatUtils.areFloatsEquals(lhs.getCarbs(), rhs.getCarbs())
                && FloatUtils.areFloatsEquals(lhs.getCalories(), rhs.getCalories())) {
            return true;
        } else {
            return false;
        }
    }

    public Foodstuff recreateWithWeight(double weight) {
        return new Foodstuff(name, weight, protein, fats, carbs, calories);
    }
}
