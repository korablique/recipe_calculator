package korablique.recipecalculator.ui.mainactivity.mainscreen;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.dagger.FragmentScope;

/**
 * Сообщает всем желающим контроллерам main screen'а о том, что тот готов к работе и
 * взаимодействию с пользователем - все нужные данные из БД стянуты, основные элементы UI
 * проинициализированны и т.д.
 */
@FragmentScope
public class MainScreenReadinessDispatcher {
    private final List<Runnable> readinessCallbacks = new ArrayList<>();
    private boolean isMainScreenReady;

    @Inject
    public MainScreenReadinessDispatcher() {
    }

    /**
     * Должен вызываться _только_ из MainScreenController.
     */
    public void onMainScreenReady() {
        if (isMainScreenReady) {
            throw new IllegalStateException("onMainScreenReady called twice!");
        }
        isMainScreenReady = true;
        for (Runnable callback : readinessCallbacks) {
            callback.run();
        }
        readinessCallbacks.clear();
    }

    /**
     * Вызывает переданный колбек как только main screen становится готов к работе.
     * Если во время вызова runWhenReady main screen уже готов к работе, колбек
     * вызывается сразу же, синхронно.
     */
    public void runWhenReady(Runnable callback) {
        if (isMainScreenReady) {
            callback.run();
            return;
        }
        readinessCallbacks.add(callback);
    }
}
