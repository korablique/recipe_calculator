package korablique.recipecalculator.model;

import java.util.Objects;

import korablique.recipecalculator.FloatUtils;

public class UserParameters {
    private final String goal;
    private final String gender;
    private final int age;
    private final int height;
    private final int weight;
    private final float physicalActivityCoefficient;
    private final String formula;

    public UserParameters(
            String goal,
            String gender,
            int age,
            int height,
            int weight,
            float physicalActivityCoefficient,
            String formula) {
        this.goal = goal;

        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.physicalActivityCoefficient = physicalActivityCoefficient;
        this.formula = formula;
    }

    public String getGoal() {
        return goal;
    }

    public String getGender() {
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

    public float getPhysicalActivityCoefficient() {
        return physicalActivityCoefficient;
    }

    public String getFormula() {
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
                FloatUtils.areFloatsEquals(that.physicalActivityCoefficient, physicalActivityCoefficient) &&
                Objects.equals(goal, that.goal) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(formula, that.formula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goal, gender, age, height, weight, physicalActivityCoefficient, formula);
    }
}

