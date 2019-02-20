package korablique.recipecalculator.database.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import korablique.recipecalculator.database.UserParametersContract;

import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_DATE_OF_BIRTH;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_LIFESTYLE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_TARGET_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

@Entity(tableName = USER_PARAMETERS_TABLE_NAME)
public class UserParametersEntity {
    @ColumnInfo(name = UserParametersContract.ID)
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = COLUMN_NAME_TARGET_WEIGHT)
    private float targetWeight;

    @ColumnInfo(name = COLUMN_NAME_GENDER)
    private int genderId;

    @NonNull
    @ColumnInfo(name = COLUMN_NAME_DATE_OF_BIRTH)
    private String dateOfBirth;

    @ColumnInfo(name = COLUMN_NAME_HEIGHT)
    private int height;

    @ColumnInfo(name = COLUMN_NAME_USER_WEIGHT)
    private float weight;

    @ColumnInfo(name = COLUMN_NAME_LIFESTYLE)
    private int lifestyleId;

    @ColumnInfo(name = COLUMN_NAME_FORMULA)
    private int formulaId;

    public UserParametersEntity(
            float targetWeight,
            int genderId,
            @NonNull String dateOfBirth,
            int height,
            float weight,
            int lifestyleId,
            int formulaId) {
        this.targetWeight = targetWeight;
        this.genderId = genderId;
        this.dateOfBirth = dateOfBirth;
        this.height = height;
        this.weight = weight;
        this.lifestyleId = lifestyleId;
        this.formulaId = formulaId;
    }

    public float getTargetWeight() {
        return targetWeight;
    }

    public void setTargetWeight(float targetWeight) {
        this.targetWeight = targetWeight;
    }

    public int getGenderId() {
        return genderId;
    }

    public void setGenderId(int genderId) {
        this.genderId = genderId;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
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
