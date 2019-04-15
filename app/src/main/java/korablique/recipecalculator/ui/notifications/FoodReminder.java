package korablique.recipecalculator.ui.notifications;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.ui.history.HistoryActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

public class FoodReminder {
    public static final String ANDROID_CHANNEL_ID = "korablique.recipecalculator.ANDROID";
    public static final int BREAKFAST_NOTIFICATION_ID = 1;
    private Context context;
    private TimeProvider timeProvider;

    public static class Time {
        private int hour;
        private int minutes;

        public Time(int hour, int minutes) {
            this.hour = hour;
            this.minutes = minutes;
        }

        public int getHour() {
            return hour;
        }

        public int getMinutes() {
            return minutes;
        }
    }

    public FoodReminder(Context context, TimeProvider timeProvider) {
        this.context = context;
        this.timeProvider = timeProvider;
    }

    public void scheduleNotification() {
        Time[] notificationTimes = new Time[3];
        notificationTimes[0] = new Time(10, 0);
        notificationTimes[1] = new Time(15, 0);
        notificationTimes[2] = new Time(20, 0);
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(timeProvider.nowUtc().getMillis());
        Calendar calendar = NotificationTimeGenerator.createNextNotificationTime(current, notificationTimes);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FoodReminderReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // без повтора
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
//        // с повтором
//        alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
//                AlarmManager.INTERVAL_DAY, alarmIntent);
    }



    public void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = context.getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(ANDROID_CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(Color.GREEN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(context, HistoryActivity.class);
        // Get the PendingIntent
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ANDROID_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_products_apple_active)
                .setContentTitle(context.getString(R.string.meal))
                .setContentText(context.getString(R.string.meal_notification))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);
        builder.setContentIntent(resultPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(BREAKFAST_NOTIFICATION_ID, builder.build());
    }
}
