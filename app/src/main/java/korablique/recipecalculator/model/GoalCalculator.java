package korablique.recipecalculator.model;

public class GoalCalculator {
    private GoalCalculator() {}

    public static int percentDone(int currentWeight, int firstWeight, int targetWeight) {
        if (currentWeight == targetWeight) {
            return 100;
        }
        // первоначальным весом считается вес, который пользователь указал в первый раз в приложении,
        // поэтому при дальнейших изменениях цели он всё так же будет считаться первоначальным
        // (1 итерация)
        float coefficient = 1 - (float)(currentWeight - targetWeight) / (firstWeight - targetWeight);
        int percent = Math.round(coefficient * 100);
        // если пользователь "перевыполнил" цель - всё равно показывать 100% (1 итерация)
        if (percent > 100) {
            percent = 100;
        }
        // если пользователь хотел похудеть, но поправился - показывать 0%
        if (percent < 0) {
            percent = 0;
        }
        return percent;
    }
}
