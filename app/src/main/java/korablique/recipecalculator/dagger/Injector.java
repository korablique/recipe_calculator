package korablique.recipecalculator.dagger;

import korablique.recipecalculator.BroccalcApplication;
import korablique.recipecalculator.base.BaseActivity;

/**
 * Интерфейс для осуществления инъекций.
 * Нужен для подмены Даггера в тестах.
 */
public interface Injector {
    void inject(BroccalcApplication application);
    void inject(BaseActivity activity);
}
