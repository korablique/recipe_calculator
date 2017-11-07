package korablique.recipecalculator.model;

public class UserParameters {
    private String goal;
    private String gender;
    private int age;
    private int height;
    private int weight;
    private float physicalActivityCoefficient;
    private String formula;

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
}
