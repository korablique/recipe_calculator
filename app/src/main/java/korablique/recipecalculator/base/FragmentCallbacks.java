package korablique.recipecalculator.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FragmentCallbacks {
    private final List<Observer> observers = new ArrayList<>();

    public interface Observer {
        default void onFragmentCreate(Bundle savedInstanceState) {}
        default void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {}
        default void onFragmentStart() {}
        default void onFragmentResume() {}
        default void onFragmentActivityResult(int requestCode, int resultCode, Intent data) {}
        default void onFragmentSaveInstanceState(Bundle outState) {}
        default void onFragmentRestoreInstanceState(Bundle savedInstanceState) {}
        default void onFragmentDestroy() {}
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

    void dispatchFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        for (Observer observer : observers) {
            observer.onFragmentViewCreated(fragmentView, savedInstanceState);
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
            observer.onFragmentActivityResult(requestCode, resultCode, data);
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
