package korablique.recipecalculator.model;

import android.content.Context;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Rates;

public class RateCalculator {
    public enum Formula {
        HARRIS_BENEDICT,
        MIFFLIN_JEOR
    }
    public enum Goal {
        LOSING_WEIGHT,
        MAINTAINING_CURRENT_WEIGHT,
        MASS_GATHERING
    }
    public enum Gender {
        MALE,
        FEMALE
    }

    private RateCalculator() {}

    public static Rates calculate(
            Context context,
            String goalString,
            String genderString,
            int age,
            int height,
            int weight,
            float physicalActivityCoefficient,
            String formulaString) {
        Goal goal = convertGoalStringToGoalEnum(context, goalString);
        Gender gender = convertGenderStringToGenderEnum(context, genderString);
        Formula formula = convertFormulaStringToFormulaEnum(context, formulaString);
        return calculate(goal, gender, age, height, weight, physicalActivityCoefficient, formula);
    }

    public static Rates calculate(Context context, UserParameters userParameters) {
        return calculate(
                context,
                userParameters.getGoal(),
                userParameters.getGender(),
                userParameters.getAge(),
                userParameters.getHeight(),
                userParameters.getWeight(),
                userParameters.getPhysicalActivityCoefficient(),
                userParameters.getFormula());
    }

    public static Rates calculate(
            Goal goal,
            Gender gender,
            int age,
            int height,
            int weight,
            float physicalActivityCoefficient,
            Formula formula) {
        // 1) рассчитываем базальный метаболизм
        float basalMetabolism;
        // формула Харриса-Бенедикта
        if (formula == Formula.HARRIS_BENEDICT) {
            if (gender == Gender.MALE) {
                // Men: 88,362 + (13,397 × вес [кг]) + (4,799 × рост [см]) − (5,677 × возраст [лет]).
                basalMetabolism = 88.362f + 13.397f * weight + 4.799f * height - 5.677f * age;
            } else {
                // Women: 447,593 + (9,247 × вес [кг]) + (3,098 × рост [см]) − (4,33 × возраст [лет]).
                basalMetabolism = 447.593f + 9.247f * weight + 3.098f * height - 4.33f * age;
            }
        } else {
            // формула Миффлина-Джеора
            if (gender == Gender.MALE) {
                // Men: 5 + (10 × вес [кг]) + (6,25 × рост [см]) − (5 × возраст [лет]).
                basalMetabolism = 5 + 10 * weight + 6.25f * height - 5 * age;
            } else {
                // Women: (10 × вес [кг]) + (6,25 × рост [см]) − (5 × возраст [лет]) − 161.
                basalMetabolism = 10 * weight + 6.25f * height - 5 * age - 161;
            }
        }

        // 2) корректируем калорийность в зависимости от активности (умножаем на коэф. активности)
        // (это поддержка)
        float calories = basalMetabolism * physicalActivityCoefficient;

        float caloriesDependingOnGoal;
        // 3) корректируем калорийность в зависимости от целей
        if (goal == Goal.LOSING_WEIGHT) {
            // если цель похудеть - берём 10% дефицит
            caloriesDependingOnGoal = calories * 0.9f;
        } else if (goal == Goal.MAINTAINING_CURRENT_WEIGHT) {
            caloriesDependingOnGoal = calories;
        } else {
            // если массонабор - 10% профицит
            caloriesDependingOnGoal = calories * 1.1f;
        }

        // 4) рассчитываем БЖУ
        // пока что будут белки 2 г/кг, жиры 1 г/кг
        float protein = weight * 2;
        float fats = weight;
        float carbs = (caloriesDependingOnGoal - (protein * 4 + fats * 9)) / 4;
        return new Rates(caloriesDependingOnGoal, protein, fats, carbs);
    }

    private static Formula convertFormulaStringToFormulaEnum(Context context, String formulaString) {
        if (formulaString.equals(context.getResources().getStringArray(R.array.formula_array)[0])) {
            return Formula.HARRIS_BENEDICT;
        } else {
            return Formula.MIFFLIN_JEOR;
        }
    }

    private static Gender convertGenderStringToGenderEnum(Context context, String genderString) {
        if (genderString.equals(context.getResources().getStringArray(R.array.gender_array)[1])) {
            return Gender.MALE;
        } else if (genderString.equals(context.getResources().getStringArray(R.array.gender_array)[2])) {
            return Gender.FEMALE;
        } else {
            throw new IllegalStateException("Gender was not selected");
        }
    }

    private static Goal convertGoalStringToGoalEnum(Context context, String goalString) {
        if (goalString.equals(context.getResources().getStringArray(R.array.goals_array)[0])) {
            return Goal.LOSING_WEIGHT;
        } else if (goalString.equals(context.getResources().getStringArray(R.array.goals_array)[1])) {
            return Goal.MAINTAINING_CURRENT_WEIGHT;
        } else {
            return Goal.MASS_GATHERING;
        }
    }
}
