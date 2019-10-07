package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.view.View;

import com.google.android.material.behavior.SwipeDismissBehavior;

/**
 * Интерфейс для удобного наследования от SwipeDismissBehavior.OnDismissListener -
 * все методы имеют пустую default-реализацию, наследоваться удобно.
 */
public interface SwipeDismissBehaviourListener extends SwipeDismissBehavior.OnDismissListener {
    default void onDismiss(View view) {}
    default void onDragStateChanged(int state) {}
}
