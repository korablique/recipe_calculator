package korablique.recipecalculator.ui.notifications;


import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class NotificationTimeGeneratorTest {
    @Test
    public void nextNotificationTimeComputesCorrectly() {
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.clear();
        todayCalendar.set(2000, 0, 1); // 1 Jan 2000
        int today = todayCalendar.get(Calendar.DAY_OF_YEAR);

        // времена для нотификаций
        FoodReminder.Time[] notificationTimes = new FoodReminder.Time[3];
        notificationTimes[0] = new FoodReminder.Time(10, 0);
        notificationTimes[1] = new FoodReminder.Time(15, 0);
        notificationTimes[2] = new FoodReminder.Time(20, 0);

        // сейчас меньше 10 часов
        todayCalendar.set(Calendar.HOUR_OF_DAY, 9);
        todayCalendar.set(Calendar.MINUTE, 15);
        Calendar calendar1 = NotificationTimeGenerator.createNextNotificationTime(todayCalendar, notificationTimes);
        Assert.assertEquals(calendar1.get(Calendar.DAY_OF_YEAR), today);
        Assert.assertEquals(calendar1.get(Calendar.HOUR_OF_DAY), 10);
        Assert.assertEquals(calendar1.get(Calendar.MINUTE), 0);

        // сейчас меньше 15 часов
        todayCalendar.set(Calendar.HOUR_OF_DAY, 12);
        todayCalendar.set(Calendar.MINUTE, 0);
        Calendar calendar2 = NotificationTimeGenerator.createNextNotificationTime(todayCalendar, notificationTimes);
        Assert.assertEquals(calendar2.get(Calendar.DAY_OF_YEAR), today);
        Assert.assertEquals(calendar2.get(Calendar.HOUR_OF_DAY), 15);
        Assert.assertEquals(calendar2.get(Calendar.MINUTE), 0);

        // сейчас меньше 20 часов
        todayCalendar.set(Calendar.HOUR_OF_DAY, 15);
        todayCalendar.set(Calendar.MINUTE, 0);
        Calendar calendar3 = NotificationTimeGenerator.createNextNotificationTime(todayCalendar, notificationTimes);
        Assert.assertEquals(calendar3.get(Calendar.DAY_OF_YEAR), today);
        Assert.assertEquals(calendar3.get(Calendar.HOUR_OF_DAY), 20);
        Assert.assertEquals(calendar3.get(Calendar.MINUTE), 0);

        // сейчас больше 20 часов (нотификация на след. день)
        Calendar nextDayCalendar = Calendar.getInstance();
        nextDayCalendar.set(
                todayCalendar.get(Calendar.YEAR),
                todayCalendar.get(Calendar.MONTH),
                todayCalendar.get(Calendar.DAY_OF_YEAR));
        nextDayCalendar.add(Calendar.DAY_OF_YEAR, 1);

        todayCalendar.set(Calendar.HOUR_OF_DAY, 22);
        todayCalendar.set(Calendar.MINUTE, 30);
        Calendar calendar4 = NotificationTimeGenerator.createNextNotificationTime(todayCalendar, notificationTimes);
        Assert.assertEquals(calendar4.get(Calendar.DAY_OF_YEAR), nextDayCalendar.get(Calendar.DAY_OF_YEAR));
        Assert.assertEquals(calendar4.get(Calendar.HOUR_OF_DAY), 10);
        Assert.assertEquals(calendar4.get(Calendar.MINUTE), 0);
    }

    @Test
    public void schedulingAtTheEndOfYearSchedulesToNextYear() {
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.clear();
        todayCalendar.set(2000, 11, 31, 21, 15); // 31 Dec 2000 21:15
        todayCalendar.set(Calendar.HOUR_OF_DAY, 21);
        todayCalendar.set(Calendar.MINUTE, 15);

        FoodReminder.Time[] notificationTimes = new FoodReminder.Time[3];
        notificationTimes[0] = new FoodReminder.Time(10, 0);
        notificationTimes[1] = new FoodReminder.Time(15, 0);
        notificationTimes[2] = new FoodReminder.Time(20, 0);

        Calendar nextDayCalendar = Calendar.getInstance();
        nextDayCalendar.clear();
        nextDayCalendar.set(2001, 0, 1);

        Calendar calendar = NotificationTimeGenerator.createNextNotificationTime(todayCalendar, notificationTimes);
        Assert.assertEquals(nextDayCalendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR));
        Assert.assertEquals(nextDayCalendar.get(Calendar.MONTH), calendar.get(Calendar.MONTH));
        Assert.assertEquals(nextDayCalendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(10, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, calendar.get(Calendar.MINUTE));
    }
}
