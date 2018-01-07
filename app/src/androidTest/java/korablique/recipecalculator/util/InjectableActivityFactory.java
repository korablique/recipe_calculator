package korablique.recipecalculator.util;

import android.content.Intent;
import android.support.test.runner.intercepting.SingleActivityFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javax.inject.Inject;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityInjector;

/**
 * Espresso во время тестирования Активити их сам создаёт и инициилизирует, но позволяет нам вмешаться
 * в процесс создания, если мы дадим ему свою фабрику Активити - SingleActivityFactory.
 * <p>
 * В тестах нам требуется подсовывать ненастоящие Dagger-зависимости в Активити до вызова onCreate(),
 * Espresso позволяет это сделать только взяв на себя создание Активити (передачей SingleActivityFactory)
 * с ручным созданием и инициилизацией Активити.
 * <p>
 * Данный класс является наследником SingleActivityFactory, и создан специально для возможности
 * создания Активити с вручную проинициилизированными @Inject-полями.
 */
class InjectableActivityFactory<T extends BaseActivity> extends SingleActivityFactory<T> {
    private final ActivityInjector<T> injectionPerformer;

    InjectableActivityFactory(
            Class<T> activityClassToIntercept, ActivityInjector<T> injectionPerformer) {
        super(activityClassToIntercept);
        this.injectionPerformer = injectionPerformer;
    }

    @Override
    protected T create(Intent intent) {
        T activity = createActivity();

        activity.setIntent(intent);
        setInjectionPerformerTo(activity);

        return activity;
    }

    private T createActivity() {
        Constructor<T> constructor;
        try {
            constructor = getActivityClassToIntercept().getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("All activities must have constructors with 0 arguments", e);
        }

        T activity;
        try {
            activity = constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't create Activity", e);
        }
        return activity;
    }

    private void setInjectionPerformerTo(T activity) {
        // В данном методе мы могли бы сделать только 1 вызов:
        // 'activity.setInjector(injectionPerformer)`,
        // но тогда мы бы не смогли затем проверить, что все @Inject-поля проинициилизированы.
        // Чтобы осуществить такую проверку, мы создаём свой ActivityInjector и уже внутри
        // него используем объект, который нам передали в конструкторе.

        ActivityInjector<T> realInjectionPerformer = (T injectedActivity) -> {
            // Выполняем инъекцию.
            injectionPerformer.inject(injectedActivity);

            // Убеждаемся, что все @Inject-поля были заданы.
            for (Field field : injectedActivity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    // Если поле не public - без вызова setAccessible мы получим тут SecurityException.
                    field.setAccessible(true);

                    Object fieldValue;
                    try {
                        fieldValue = field.get(injectedActivity);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(
                                "Couldn't check @Inject field value", e);
                    }

                    if (fieldValue == null) {
                        throw new IllegalStateException(
                                "Given injector didn't set the '" + field.getName() + "' field!");
                    }
                }
            }
        };

        activity.setInjector(realInjectionPerformer);
    }
}
