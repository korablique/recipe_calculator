package korablique.recipecalculator.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class UserParameters implements Parcelable {
    private final Goal goal;
    private final Gender gender;
    private final int age;
    private final int height;
    private final int weight;
    private final Lifestyle lifestyle;
    private final Formula formula;

    public UserParameters(
            Goal goal,
            Gender gender,
            int age,
            int height,
            int weight,
            Lifestyle lifestyle,
            Formula formula) {
        this.goal = goal;
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.lifestyle = lifestyle;
        this.formula = formula;
    }

    protected UserParameters(Parcel in) {
        goal = (Goal) in.readSerializable();
        gender = (Gender) in.readSerializable();
        age = in.readInt();
        height = in.readInt();
        weight = in.readInt();
        lifestyle = (Lifestyle) in.readSerializable();
        formula = (Formula) in.readSerializable();
    }

    public static final Creator<UserParameters> CREATOR = new Creator<UserParameters>() {
        @Override
        public UserParameters createFromParcel(Parcel in) {
            return new UserParameters(in);
        }

        @Override
        public UserParameters[] newArray(int size) {
            return new UserParameters[size];
        }
    };

    public Goal getGoal() {
        return goal;
    }

    public Gender getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }

    public int getHeight() {
        return height;
    }

    public int getWeight() {
        return weight;
    }

    public Lifestyle getLifestyle() {
        return lifestyle;
    }

    public Formula getFormula() {
        return formula;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserParameters that = (UserParameters) o;
        return age == that.age &&
                height == that.height &&
                weight == that.weight &&
                Objects.equals(lifestyle, that.lifestyle) &&
                Objects.equals(goal, that.goal) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(formula, that.formula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goal, gender, age, height, weight, lifestyle, formula);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(goal);
        parcel.writeSerializable(gender);
        parcel.writeInt(age);
        parcel.writeInt(height);
        parcel.writeInt(weight);
        parcel.writeSerializable(lifestyle);
        parcel.writeSerializable(formula);
    }
}

