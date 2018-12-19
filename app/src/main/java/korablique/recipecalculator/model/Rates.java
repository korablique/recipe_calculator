package korablique.recipecalculator.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Rates implements Parcelable {
    private float calories;
    private float protein;
    private float fats;
    private float carbs;

    public Rates(float calories, float protein, float fats, float carbs) {
        this.calories = calories;
        this.protein = protein;
        this.fats = fats;
        this.carbs = carbs;
    }

    protected Rates(Parcel in) {
        calories = in.readFloat();
        protein = in.readFloat();
        fats = in.readFloat();
        carbs = in.readFloat();
    }

    public static final Creator<Rates> CREATOR = new Creator<Rates>() {
        @Override
        public Rates createFromParcel(Parcel in) {
            return new Rates(in);
        }

        @Override
        public Rates[] newArray(int size) {
            return new Rates[size];
        }
    };

    public float getCalories() {
        return calories;
    }

    public float getProtein() {
        return protein;
    }

    public float getFats() {
        return fats;
    }

    public float getCarbs() {
        return carbs;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(calories);
        parcel.writeFloat(protein);
        parcel.writeFloat(fats);
        parcel.writeFloat(carbs);
    }
}
