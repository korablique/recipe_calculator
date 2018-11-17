package korablique.recipecalculator.model;

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
    public boolean equals(Object other) {
        if (!(other instanceof UserParameters)) {
            return false;
        }

        UserParameters otherParams = (UserParameters) other;
        return goal.equals(otherParams.goal)
                && gender.equals(otherParams.gender)
                && age == otherParams.age
                && height == otherParams.height
                && weight == otherParams.weight
                && physicalActivityCoefficient == otherParams.physicalActivityCoefficient
                && formula.equals(otherParams.formula);
    }

    @Override
    public int hashCode() {
        // NOTE: hash code is terrible, but the class is not stored in collections.
        return age;
    }
}
