package korablique.recipecalculator.util;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.util.TestingInjector.ActivitiesInjectionSource;
import korablique.recipecalculator.util.TestingInjector.SingletonInjectionsSource;

/**
 * Rule для Espresso, которое необходимо использовать вместо ActivityTestRule, если Активити
 * содержит какие-либо @Inject-поля, которые требуется проинициилизировать самостоятельно,
 * мимо Даггера.
 */
public class InjectableActivityTestRule<T extends BaseActivity> extends ActivityTestRule<T> {
    @Nullable
    private final SingletonInjectionsSource singletonInjectionsSource;
    @Nullable
    private final ActivitiesInjectionSource activitiesInjectionSource;

    public static <BT extends BaseActivity> Builder<BT> forActivity(Class<BT> activityClass) {
        return new Builder<>(activityClass);
    }

    private InjectableActivityTestRule(Builder<T> builder) {
        super(builder.activityClass, false /* initialTouchMode */, builder.shouldStartImmediately);
        this.singletonInjectionsSource = builder.singletonInjectionsSource;
        this.activitiesInjectionSource = builder.activitiesInjectionSource;
    }

    private void onTestStarted() {
        // Initializing espresso-intents
        Intents.init();
        InjectorHolder.setInjector(
                new TestingInjector(singletonInjectionsSource, activitiesInjectionSource));
    }

    private void onTestEnded() {
        Intents.release();
        InjectorHolder.setInjector(null);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new MyStatement(super.apply(base, description));
    }

    // Паттерн 'Builder'.
    public static class Builder<BT extends BaseActivity> {
        private final Class<BT> activityClass;
        @Nullable
        private SingletonInjectionsSource singletonInjectionsSource;
        @Nullable
        private ActivitiesInjectionSource activitiesInjectionSource;
        private boolean shouldStartImmediately = true;

        private Builder(Class<BT> activityClass) {
            this.activityClass = activityClass;
        }

        /**
         * Устанавливаем форсированный ручной старт - активити не стартует сама до вызова
         * {@link ActivityTestRule#launchActivity(Intent)}.
         */
        public Builder<BT> withManualStart() {
            this.shouldStartImmediately = false;
            return this;
        }

        /**
         * Устанавливаем источник синглтонов.
         */
        public Builder<BT> withSingletones(SingletonInjectionsSource source) {
            this.singletonInjectionsSource = source;
            return this;
        }

        /**
         * Устанавливаем источник ActivityScoped зависимостей.
         */
        public Builder<BT> withActivityScoped(ActivitiesInjectionSource source) {
            this.activitiesInjectionSource = source;
            return this;
        }

        public InjectableActivityTestRule<BT> build() {
            return new InjectableActivityTestRule<>(this);
        }
    }

    private class MyStatement extends Statement {
        private final Statement base;
        MyStatement(Statement base) {
            this.base = base;
        }
        @Override
        public void evaluate() throws Throwable {
            onTestStarted();
            try {
                base.evaluate();
            } finally {
                onTestEnded();
            }
        }
    }
}
