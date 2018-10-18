package korablique.recipecalculator.dagger;

import android.support.v4.app.Fragment;

import korablique.recipecalculator.BroccalcApplication;
import korablique.recipecalculator.base.BaseActivity;

/**
 * Интерфейс для осуществления инъекций.
 * Нужен для подмены Даггера в тестах.
 */
public interface Injector {
    void inject(BroccalcApplication application);
    void inject(BaseActivity activity);
    void inject(Fragment fragment);
}
