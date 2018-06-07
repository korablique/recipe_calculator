package korablique.recipecalculator.util;

import android.content.Intent;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityInjector;

/**
 * Rule для Espresso, которое необходимо использовать вместо ActivityTestRule, если Активити
 * содержит какие-либо @Inject-поля, которые требуется проинициилизировать самостоятельно,
 * мимо Даггера.
 */
public class InjectableActivityTestRule<T extends BaseActivity> extends ActivityTestRule<T> {
    private boolean isInitialized;

    // Паттерн 'Builder'.
    public static class Builder<BT extends BaseActivity> {
        private final Class<BT> activityClass;
        private ActivityInjector injector;
        private boolean shouldStartImmediately = true;

        private Builder(Class<BT> activityClass) {
            this.activityClass = activityClass;
        }

        /**
         * Задаем свой инжектор.
         */
        public Builder<BT> withInjector(ActivityInjector<BT> injector) {
            this.injector = injector;
            return this;
        }

        /**
         * Устанавливаем форсированный ручной старт - активити не стартует сама до вызова
         * {@link ActivityTestRule#launchActivity(Intent)}.
         */
        public Builder<BT> withManualStart() {
            this.shouldStartImmediately = false;
            return this;
        }

        public InjectableActivityTestRule<BT> build() {
            if (injector == null) {
                throw new IllegalStateException("withInjector() was not called");
            }
            return new InjectableActivityTestRule<>(this);
        }
    }

    public static <BT extends BaseActivity> Builder<BT> forActivity(Class<BT> activityClass) {
        return new Builder<>(activityClass);
    }

    private InjectableActivityTestRule(Builder<T> builder) {
        super(new InjectableActivityFactory<T>(builder.activityClass, builder.injector),
                false,
                builder.shouldStartImmediately);
    }

    @Override
    protected void afterActivityLaunched() {
        // Initializing espresso-intents
        Intents.init();
        isInitialized = true;
        super.afterActivityLaunched();
    }

    @Override
    protected void afterActivityFinished() {
        // Deinitializing espresso-intents
        super.afterActivityFinished();
        if (isInitialized) {
            Intents.release();
            isInitialized = false;
        }
    }
}
