package korablique.recipecalculator.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

import dagger.android.AndroidInjection;


public class FoodReminderReceiver extends BroadcastReceiver {
    @Inject
    FoodReminder foodReminder;
    @Override
    public void onReceive(Context context, Intent intent) {
        AndroidInjection.inject(this, context);

        foodReminder.showNotification();
        foodReminder.scheduleNotification();
    }
}
