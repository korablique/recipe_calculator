package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class ActivityCallbacks {
    private final List<Observer> observers = new ArrayList<>();

    public static abstract class Observer {
        public void onActivityCreate(Bundle savedInstanceState) {}
        public void onActivityResume() {}
        public void onActivityPause() {}
        public void onActivityDestroy() {}
        public void onActivityResult(int requestCode, int resultCode, Intent data) {}
        public void onActivitySaveInstanceState(Bundle outState) {}
        public void onActivityRestoreInstanceState(Bundle savedInstanceState) {}
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
}
