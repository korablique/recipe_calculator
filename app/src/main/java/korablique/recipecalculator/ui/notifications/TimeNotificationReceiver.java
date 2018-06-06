package korablique.recipecalculator.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class TimeNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        FoodReminderComponent foodReminderComponent = new FoodReminderComponent(context);
        foodReminderComponent.showNotification();
        foodReminderComponent.scheduleNotification();
    }
}
