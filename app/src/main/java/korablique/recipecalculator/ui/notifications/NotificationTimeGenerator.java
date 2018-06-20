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
            Calendar currentTime, FoodReminder.Time... notificationTimes) {
        int currentTimeInMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE);
        FoodReminder.Time nextNotificationTime = null;
        for (FoodReminder.Time time : notificationTimes) {
            int timeInMinutes = time.getHour() * 60 + time.getMinutes();
            if (currentTimeInMinutes < timeInMinutes) {
                nextNotificationTime = time;
                break;
            }
        }
        Calendar calendar = Calendar.getInstance();
        // задаем возвращаемому календарю тот же год, месяц и день, что и у текущего
        calendar.set(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH), currentTime.get(Calendar.DAY_OF_MONTH));
        // если текущее время _больше_ предложенных времен для нотификаций
        if (nextNotificationTime == null) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            calendar.add(Calendar.HOUR_OF_DAY, notificationTimes[0].getHour());
            calendar.add(Calendar.MINUTE, notificationTimes[0].getMinutes());
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, nextNotificationTime.getHour());
            calendar.set(Calendar.MINUTE, nextNotificationTime.getMinutes());
        }
        return calendar;
    }
}
