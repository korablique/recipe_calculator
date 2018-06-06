package korablique.recipecalculator.ui.notifications;


import java.util.Calendar;

public class NotificationTimeGenerator {
    /**
     *
     * @param currentTime
     * @param notificationTimes набор времен суток для показа нотификаций (должен быть отсортирован по возрастанию)
     * @return возвращает время показа для следующей нотификации
     */
    public static Calendar createNextNotificationTime(
            FoodReminderComponent.Time currentTime, FoodReminderComponent.Time... notificationTimes) {
        int currentTimeInMinutes = currentTime.getHour() * 60 + currentTime.getMinutes();
        FoodReminderComponent.Time nextNotificationTime = null;
        for (FoodReminderComponent.Time time : notificationTimes) {
            int timeInMinutes = time.getHour() * 60 + time.getMinutes();
            if (currentTimeInMinutes < timeInMinutes) {
                nextNotificationTime = time;
                break;
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // если текущее время _больше_ предложенных времен для нотификаций
        if (nextNotificationTime == null) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.add(Calendar.HOUR_OF_DAY, notificationTimes[0].getHour());
            calendar.add(Calendar.MINUTE, notificationTimes[0].getMinutes());
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, nextNotificationTime.getHour());
            calendar.set(Calendar.MINUTE, nextNotificationTime.getMinutes());
        }
        return calendar;
    }
}
