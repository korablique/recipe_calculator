package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class ActivityCallbacks {
    private final List<Observer> observers = new ArrayList<>();

    public interface Observer {
        default void onActivityCreate(Bundle savedInstanceState) {}
        default void onActivityNewIntent(Intent intent) {}
        default void onActivityStart() {}
        default void onActivityResume() {}
        default void onActivityPause() {}
        default void onActivityStop() {}
        default void onActivityDestroy() {}
        default void onActivityResult(int requestCode, int resultCode, Intent data) {}
        default void onActivitySaveInstanceState(Bundle outState) {}
        default void onActivityRestoreInstanceState(Bundle savedInstanceState) {}
        /**
         * @return true if event was consumed and Activity should not finish on back
         */
        default boolean onActivityBackPressed() { return false; }
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    void dispatchActivityCreate(Bundle savedInstanceState) {
        for (Observer observer : observers) {
            observer.onActivityCreate(savedInstanceState);
        }
    }

    void dispatchActivityNewIntent(Intent intent) {
        for (Observer observer : observers) {
            observer.onActivityNewIntent(intent);
        }
    }

    void dispatchActivityStart() {
        for (Observer observer : observers) {
            observer.onActivityStart();
        }
    }

    void dispatchActivityResume() {
        for (Observer observer : observers) {
            observer.onActivityResume();
        }
    }

    void dispatchActivityPause() {
        for (Observer observer : observers) {
            observer.onActivityPause();
        }
    }

    void dispatchActivityStop() {
        for (Observer observer : observers) {
            observer.onActivityStop();
        }
    }

    void dispatchActivityDestroy() {
        for (Observer observer : observers) {
            observer.onActivityDestroy();
        }
    }

    void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        for (Observer observer : observers) {
            observer.onActivityResult(requestCode, resultCode, data);
        }
    }

    void dispatchSaveInstanceState(Bundle outState) {
        for (Observer observer : observers) {
            observer.onActivitySaveInstanceState(outState);
        }
    }

    void dispatchRestoreInstanceState(Bundle savedInstanceState) {
        for (Observer observer : observers) {
            observer.onActivityRestoreInstanceState(savedInstanceState);
        }
    }

    boolean dispatchActivityBackPressed() {
        for (Observer observer : observers) {
            boolean eventConsumed = observer.onActivityBackPressed();
            if (eventConsumed) {
                return true;
            }
        }
        return false;
    }
}
