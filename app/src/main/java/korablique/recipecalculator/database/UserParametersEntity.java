package korablique.recipecalculator.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import static korablique.recipecalculator.database.UserParametersContract.*;

@Entity(tableName = USER_PARAMETERS_TABLE_NAME)
class UserParametersEntity {
    @ColumnInfo(name = UserParametersContract.ID)
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = COLUMN_NAME_GOAL)
    private int goalId;

    @ColumnInfo(name = COLUMN_NAME_GENDER)
    private int genderId;

    @ColumnInfo(name = COLUMN_NAME_AGE)
    private int age;

    @ColumnInfo(name = COLUMN_NAME_HEIGHT)
    private int height;

    @ColumnInfo(name = COLUMN_NAME_USER_WEIGHT)
    private int weight;

    @ColumnInfo(name = COLUMN_NAME_LIFESTYLE)
    private int lifestyleId;

    @ColumnInfo(name = COLUMN_NAME_FORMULA)
    private int formulaId;

    public UserParametersEntity(
            int goalId,
            int genderId,
            int age,
            int height,
            int weight,
            int lifestyleId,
            int formulaId) {
        this.goalId = goalId;
        this.genderId = genderId;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.lifestyleId = lifestyleId;
        this.formulaId = formulaId;
    }

    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public int getGenderId() {
        return genderId;
    }

    public void setGenderId(int genderId) {
        this.genderId = genderId;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getLifestyleId() {
        return lifestyleId;
    }

    public void setLifestyleId(int lifestyleId) {
        this.lifestyleId = lifestyleId;
    }

    public int getFormulaId() {
        return formulaId;
    }

    public void setFormulaId(int formulaId) {
        this.formulaId = formulaId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
