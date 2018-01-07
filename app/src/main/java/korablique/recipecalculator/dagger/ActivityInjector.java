package korablique.recipecalculator.dagger;

import korablique.recipecalculator.base.BaseActivity;

/**
 * Интерфейс для осуществления инъекций в Активити.
 * Нужен для подмены Даггера в тестах.
 */
public interface ActivityInjector<T extends BaseActivity> {
    void inject(T activity);
}
