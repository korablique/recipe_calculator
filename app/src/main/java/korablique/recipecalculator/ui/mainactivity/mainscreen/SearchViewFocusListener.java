package korablique.recipecalculator.ui.mainactivity.mainscreen;

import com.arlib.floatingsearchview.FloatingSearchView;

/**
 * Вспомогательный интерфейс с пустыми default-методами.
 */
public interface SearchViewFocusListener extends FloatingSearchView.OnFocusChangeListener {
    default void onFocus() {}
    default void onFocusCleared() {}
}
