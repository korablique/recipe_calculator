package korablique.recipecalculator.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.base.TimeProvider;

/**
 * The class does 2 things:
 *
 * 1. Watches the app going to background and back to foreground, if the app was in the background
 *    for a long time (for MAX_SESSION_LENGTH), notifies observers that a new session was started.
 * 2. Helps its clients (SessionClient) to start new sessions when they need to. To do that,
 *    the class stores in Preferences information about whether the clients should reset their state
 *    due to new session start.
 */
@Singleton
public class SessionController implements CurrentActivityProvider.Observer {
    public static final long MAX_SESSION_LENGTH = TimeUnit.HOURS.toMillis(3);
    private static final String PREFERENCES_ID = "SessionController";
    private static final String PREF_LAST_SHUTDOWN_TIME = "LAST_SHUTDOWN_TIME";
    private static final String PREF_CLIENT_SESSION_STATUS_ = "CLIENT_SESSION_STATUS_";

    private static final String PREF_VAL_CLIENT_SESSION_OVER = "OVER";
    private static final String PREF_VAL_CLIENT_SESSION_ACTIVE = "ACTIVE";

    private final Context context;
    private final TimeProvider timeProvider;
    private final List<Observer> observers = new ArrayList<>();

    public interface Observer {
        void onNewSession();
    }

    @Inject
    public SessionController(
            Context context,
            TimeProvider timeProvider,
            CurrentActivityProvider currentActivityProvider) {
        this.context = context;
        this.timeProvider = timeProvider;
        currentActivityProvider.addObserver(this);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public boolean shouldStartNewSessionFor(SessionClient client) {
        String prefName = PREF_CLIENT_SESSION_STATUS_ + client.name;
        String defaultVal = PREF_VAL_CLIENT_SESSION_OVER;
        String clientSessionStatus = prefs().getString(prefName, defaultVal);
        return PREF_VAL_CLIENT_SESSION_OVER.equals(clientSessionStatus);
    }

    public void onClientStartedNewSession(SessionClient client) {
        String prefName = PREF_CLIENT_SESSION_STATUS_ + client.name;
        prefs().edit()
                .putString(prefName, PREF_VAL_CLIENT_SESSION_ACTIVE)
                .apply();
    }

    @Override
    public void onCurrentActivityChanged(
            @Nullable BaseActivity oldActivity, @Nullable BaseActivity newActivity) {
        // App shutdown
        if (newActivity == null) {
            prefs().edit()
                    .putLong(PREF_LAST_SHUTDOWN_TIME, now())
                    .apply();
            return;
        }
        // App startup
        if (oldActivity == null && newActivity != null) {
            long lastShutdownTime = prefs().getLong(PREF_LAST_SHUTDOWN_TIME, -1);
            if (now() - lastShutdownTime > MAX_SESSION_LENGTH) {
                // Let's reset sessions of all clients
                SharedPreferences.Editor prefsEditor = prefs().edit();
                for (SessionClient client : SessionClient.values()) {
                    prefsEditor.putString(
                            PREF_CLIENT_SESSION_STATUS_ + client.name,
                            PREF_VAL_CLIENT_SESSION_OVER);
                }
                prefsEditor.apply();
                // Let's notify all observers about the new session
                for (Observer observer : observers) {
                    observer.onNewSession();
                }
            }
        }
    }

    private long now() {
        return timeProvider.nowUtc().getMillis();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE);
    }
}
