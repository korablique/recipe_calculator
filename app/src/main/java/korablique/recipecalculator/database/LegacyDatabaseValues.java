package korablique.recipecalculator.database;

import korablique.recipecalculator.util.FloatUtils;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Goal;
import korablique.recipecalculator.model.Lifestyle;

/**
 * Значения из старых версий БД, нужные для эмиграции.
 */
public class LegacyDatabaseValues {
    public static final String COLUMN_NAME_COEFFICIENT = "coefficient";
    public static final String COLUMN_NAME_GOAL = "goal";

    public static final String GOAL_LOSING_WEIGHT = "Снижение веса";
    public static final String GOAL_MAINTAINING_CURRENT_WEIGHT = "Сохранение веса";
    public static final String GOAL_MASS_GATHERING = "Набор веса";

    public static final String GENDER_MALE = "Мужской";
    public static final String GENDER_FEMALE = "Женский";

    public static final float COEFFICIENT_PASSIVE = 1.2f;
    public static final float COEFFICIENT_INSIGNIFICANT_ACTIVITY = 1.375f;
    public static final float COEFFICIENT_MEDIUM_ACTIVITY = 1.55f;
    public static final float COEFFICIENT_ACTIVE = 1.725f;
    public static final float COEFFICIENT_PROFESSIONAL_SPORTS = 1.9f;

    public static final String FORMULA_HARRIS_BENEDICT = "Харриса-Бенедикта";
    public static final String FORMULA_MIFFLIN_JEOR = "Миффлина-Джеора";

    public static Goal convertGoal(String goalString) {
        if (goalString.equals(GOAL_LOSING_WEIGHT)) {
            return Goal.LOSING_WEIGHT;
        } else if (goalString.equals(GOAL_MAINTAINING_CURRENT_WEIGHT)) {
            return Goal.MAINTAINING_CURRENT_WEIGHT;
        } else if (goalString.equals(GOAL_MASS_GATHERING)) {
            return (Goal.MASS_GATHERING);
        } else {
            throw new IllegalArgumentException("There is no such goal: " + goalString);
        }
    }

    public static Gender convertGender(String genderString) {
        if (genderString.equals(GENDER_MALE)) {
            return Gender.MALE;
        } else if (genderString.equals(GENDER_FEMALE)) {
            return Gender.FEMALE;
        } else {
            throw new IllegalArgumentException("There is no such gender: " + genderString);
        }
    }

    public static Lifestyle convertCoefficient(float coefficient) {
        if (FloatUtils.areFloatsEquals(coefficient, COEFFICIENT_PASSIVE)) {
            return Lifestyle.PASSIVE_LIFESTYLE;
        } else if (FloatUtils.areFloatsEquals(coefficient, COEFFICIENT_INSIGNIFICANT_ACTIVITY)) {
            return Lifestyle.INSIGNIFICANT_ACTIVITY;
        } else if (FloatUtils.areFloatsEquals(coefficient, COEFFICIENT_MEDIUM_ACTIVITY)) {
            return Lifestyle.MEDIUM_ACTIVITY;
        } else if (FloatUtils.areFloatsEquals(coefficient, COEFFICIENT_ACTIVE)) {
            return Lifestyle.ACTIVE_LIFESTYLE;
        } else if (FloatUtils.areFloatsEquals(coefficient, COEFFICIENT_PROFESSIONAL_SPORTS)) {
            return Lifestyle.PROFESSIONAL_SPORTS;
        } else {
            throw new IllegalArgumentException("There is no such coefficient: " + coefficient);
        }
    }

    public static Formula convertFormula(String formulaString) {
        if (formulaString.equals(FORMULA_HARRIS_BENEDICT)) {
            return Formula.HARRIS_BENEDICT;
        } else if (formulaString.equals(FORMULA_MIFFLIN_JEOR)) {
            return Formula.MIFFLIN_JEOR;
        } else {
            throw new IllegalArgumentException("There is no such coefficient: " + formulaString);
        }
    }
}
