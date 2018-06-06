package korablique.recipecalculator.ui.notifications;


import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class NotificationTimeGeneratorTest {
    @Test
    public void nextNotificationTimeComputesCorrectly() {
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.setTimeInMillis(System.currentTimeMillis());
        int today = todayCalendar.get(Calendar.DAY_OF_YEAR);

        // времена для нотификаций
        FoodReminderComponent.Time[] notificationTimes = new FoodReminderComponent.Time[3];
        notificationTimes[0] = new FoodReminderComponent.Time(10, 0);
        notificationTimes[1] = new FoodReminderComponent.Time(15, 0);
        notificationTimes[2] = new FoodReminderComponent.Time(20, 0);

        // сейчас меньше 10 часов
        FoodReminderComponent.Time currentTime1 = new FoodReminderComponent.Time(9, 15);
        Calendar calendar1 = NotificationTimeGenerator.createNextNotificationTime(currentTime1, notificationTimes);
        Assert.assertEquals(calendar1.get(Calendar.DAY_OF_YEAR), today);
        Assert.assertEquals(calendar1.get(Calendar.HOUR_OF_DAY), 10);
        Assert.assertEquals(calendar1.get(Calendar.MINUTE), 0);

        // сейчас меньше 15 часов
        FoodReminderComponent.Time currentTime2 = new FoodReminderComponent.Time(12, 0);
        Calendar calendar2 = NotificationTimeGenerator.createNextNotificationTime(currentTime2, notificationTimes);
        Assert.assertEquals(calendar2.get(Calendar.DAY_OF_YEAR), today);
        Assert.assertEquals(calendar2.get(Calendar.HOUR_OF_DAY), 15);
        Assert.assertEquals(calendar2.get(Calendar.MINUTE), 0);

        // сейчас меньше 20 часов
        FoodReminderComponent.Time currentTime3 = new FoodReminderComponent.Time(15, 0);
        Calendar calendar3 = NotificationTimeGenerator.createNextNotificationTime(currentTime3, notificationTimes);
        Assert.assertEquals(calendar3.get(Calendar.DAY_OF_YEAR), today);
        Assert.assertEquals(calendar3.get(Calendar.HOUR_OF_DAY), 20);
        Assert.assertEquals(calendar3.get(Calendar.MINUTE), 0);

        // сейчас больше 20 часов (нотификация на след. день)
        Calendar nextDayCalendar = Calendar.getInstance();
        nextDayCalendar.setTimeInMillis(System.currentTimeMillis());
        nextDayCalendar.add(Calendar.DAY_OF_YEAR, 1);

        FoodReminderComponent.Time currentTime4 = new FoodReminderComponent.Time(22, 30);
        Calendar calendar4 = NotificationTimeGenerator.createNextNotificationTime(currentTime4, notificationTimes);
        Assert.assertEquals(calendar4.get(Calendar.DAY_OF_YEAR), nextDayCalendar.get(Calendar.DAY_OF_YEAR));
        Assert.assertEquals(calendar4.get(Calendar.HOUR_OF_DAY), 10);
        Assert.assertEquals(calendar4.get(Calendar.MINUTE), 0);
    }
}
