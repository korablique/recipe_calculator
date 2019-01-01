package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FragmentCallbacks {
    private final List<Observer> observers = new ArrayList<>();

    public static abstract class Observer {
        public void onFragmentCreate(Bundle savedInstanceState) {}
        public void onFragmentViewCreated(View fragmentView) {}
        public void onFragmentStart() {}
        public void onFragmentResume() {}
        public void onActivityResult(int requestCode, int resultCode, Intent data) {}
        public void onFragmentSaveInstanceState(Bundle outState) {}
        public void onFragmentRestoreInstanceState(Bundle savedInstanceState) {}
        public void onFragmentDestroy() {}
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    void dispatchFragmentCreate(Bundle savedInstanceState) {
        for (Observer observer : observers) {
            observer.onFragmentCreate(savedInstanceState);
        }
    }

    void dispatchFragmentViewCreated(View fragmentView) {
        for (Observer observer : observers) {
            observer.onFragmentViewCreated(fragmentView);
        }
    }

    void dispatchFragmentStart() {
        for (Observer observer : observers) {
            observer.onFragmentStart();
        }
    }

    void dispatchFragmentResume() {
        for (Observer observer : observers) {
            observer.onFragmentResume();
        }
    }

    void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        for (Observer observer : observers) {
            observer.onActivityResult(requestCode, resultCode, data);
        }
    }

    void dispatchFragmentSaveInstanceState(Bundle outState) {
        for (Observer observer : observers) {
            observer.onFragmentSaveInstanceState(outState);
        }
    }

    void dispatchFragmentRestoreInstanceState(Bundle savedInstanceState) {
        for (Observer observer : observers) {
            observer.onFragmentRestoreInstanceState(savedInstanceState);
        }
    }

    void dispatchFragmentDestroy() {
        for (Observer observer : observers) {
            observer.onFragmentDestroy();
        }
    }
}
