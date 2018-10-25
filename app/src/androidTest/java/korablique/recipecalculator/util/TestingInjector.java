package korablique.recipecalculator.util;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import korablique.recipecalculator.BroccalcApplication;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.Injector;

/**
 * Injector for tests.
 */
public class TestingInjector implements Injector {
    @Nullable
    private final ActivitiesInjectionSource activitiesInjectionSource;
    private final List<Object> singletonInjections;

    /**
     * Объект, предоставляющий @Singleton-зависимости
     */
    public interface SingletonInjectionsSource {
        List<Object> create();
    }

    public interface ActivitiesInjectionSource {
        List<Object> createFor(Object injectionTarget);
    }

    public TestingInjector(
            @Nullable SingletonInjectionsSource singletonInjectionsSource,
            @Nullable ActivitiesInjectionSource activitiesInjectionSource) {
        if (singletonInjectionsSource == null && activitiesInjectionSource == null) {
            throw new IllegalStateException("All injections sources are null");
        }
        if (singletonInjectionsSource != null) {
            this.singletonInjections = singletonInjectionsSource.create();
        } else {
            this.singletonInjections = Collections.emptyList();
        }
        this.activitiesInjectionSource = activitiesInjectionSource;
    }

    @Override
    public void inject(BroccalcApplication application) {
        // Application injection not supported -
        // Application class is getting instantiated before any TestRule
    }

    @Override
    public void inject(BaseActivity activity) {
        injectImpl(activity);
    }

    @Override
    public void inject(Fragment fragment) {
        injectImpl(fragment);
    }

    private <T> void injectImpl(T target) {
        List<Object> injectedObjects = new ArrayList<>(singletonInjections);
        if (activitiesInjectionSource != null) {
            injectedObjects.addAll(activitiesInjectionSource.createFor(target));
        }

        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getType() != DispatchingAndroidInjector.class && field.isAnnotationPresent(Inject.class)) {
                injectInto(target, field, injectedObjects);
            }
        }
    }

    /**
     * @param target содержит поле
     * @param field поле, в которое нужно инжектить
     * @param injectedObjects объекты, которые требуется въинжектить (все)
     * @param <T> активити или фрагмент
     */
    private <T> void injectInto(T target, Field field, List<Object> injectedObjects) {
        // Если поле не public - без вызова setAccessible мы получим тут SecurityException.
        field.setAccessible(true);

        Object fieldValue;
        try {
            fieldValue = field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Couldn't check @Inject field value", e);
        }

        if (fieldValue != null) {
            throw new IllegalStateException(String.format(
                    "Target %s has its field %s already initialized",
                    target.getClass().getName(),
                    field.getName()));
        }

        boolean valueSet = false;
        for (Object injectedObject : injectedObjects) {
            if (field.getType().isAssignableFrom(injectedObject.getClass())) {
                try {
                    field.set(target, injectedObject);
                    valueSet = true;
                    break;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Couldn't change field's value", e);
                }
            }
        }
        if (!valueSet) {
            StringBuilder builder = new StringBuilder();
            for (Object injectedObject : injectedObjects) {
                builder.append(injectedObject.getClass().getName()).append(" ");
            }
            throw new IllegalStateException(String.format(
                    "Couldn't find object to inject into field! Field: %s, objects: %s",
                    field.getName(),
                    builder.toString()));
        }
    }
}
