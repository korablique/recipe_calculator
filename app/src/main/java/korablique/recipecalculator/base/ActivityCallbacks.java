package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class ActivityCallbacks {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

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
        // Notify observers in reversed order, so that the most recently added observers would
        // receive the event first.
        // What this is for - usually new UI elements which appear on top of the UI want to react to
        // back presses before any other UI elements. For example, if a Dialog is shown, user expects
        // that a back press would close it immediately, so the dialog needs to receive the back press
        // event first.
        ListIterator<Observer> reversedIterator =
                observers.listIterator(observers.size());
        while (reversedIterator.hasPrevious()) {
            Observer observer = reversedIterator.previous();
            boolean eventConsumed = observer.onActivityBackPressed();
            if (eventConsumed) {
                // Event consumed by current observer - other observers must not receive it.
                return true;
            }
        }
        return false;
    }
}
