package korablique.recipecalculator.dagger;

import dagger.android.AndroidInjection;
import korablique.recipecalculator.base.BaseActivity;

/**
 * Реализует инъекции в Активити посредством Даггера.
 */
public class DefaultActivityInjector<T extends BaseActivity> implements ActivityInjector<T> {
    @Override
    public void inject(T activity) {
        AndroidInjection.inject(activity);
    }
}
