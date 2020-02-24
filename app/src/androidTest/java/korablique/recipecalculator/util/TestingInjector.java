package korablique.recipecalculator.util;

import android.app.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
    @Nullable
    private final FragmentInjectionSource fragmentInjectionSource;
    private final List<Object> singletonInjections;

    private final Map<BaseActivity, List<Object>> cachedActivityInjections = new HashMap<>();
    private final Map<Fragment, List<Object>> cachedFragmentInjections = new HashMap<>();

    /**
     * Объект, предоставляющий @Singleton-зависимости
     */
    public interface SingletonInjectionsSource {
        List<Object> create();
    }

    public interface ActivitiesInjectionSource {
        List<Object> createFor(Object injectionTarget);
    }

    public interface FragmentInjectionSource {
        List<Object> createFor(Object injectionTarget);
    }

    public TestingInjector(
            @Nullable SingletonInjectionsSource singletonInjectionsSource,
            @Nullable ActivitiesInjectionSource activitiesInjectionSource,
            @Nullable FragmentInjectionSource fragmentInjectionSource) {
        if (singletonInjectionsSource == null && activitiesInjectionSource == null) {
            throw new IllegalStateException("All injections sources are null");
        }
        if (singletonInjectionsSource != null) {
            this.singletonInjections = singletonInjectionsSource.create();
        } else {
            this.singletonInjections = Collections.emptyList();
        }
        this.activitiesInjectionSource = activitiesInjectionSource;
        this.fragmentInjectionSource = fragmentInjectionSource;
    }

    @Override
    public void inject(BroccalcApplication application) {
        // Application injection not supported -
        // Application class is getting instantiated before any TestRule
    }

    @Override
    public void inject(BaseActivity activity) {
        ensureCacheExistence(activity);
        injectImpl(activity, cachedActivityInjections.get(activity));
    }

    private void ensureCacheExistence(BaseActivity activity) {
        if (!cachedActivityInjections.containsKey(activity)) {
            List<Object> injectedObjects = new ArrayList<>(singletonInjections);
            if (activitiesInjectionSource != null) {
                injectedObjects.addAll(activitiesInjectionSource.createFor(activity));
            }
            cachedActivityInjections.put(activity, injectedObjects);
        }
    }

    @Override
    public void inject(Fragment fragment) {
        ensureCacheExistence(fragment);
        injectImpl(fragment, cachedFragmentInjections.get(fragment));
    }

    @Override
    public void inject(Service service) {
        // Not supported yet
    }

    private void ensureCacheExistence(Fragment fragment) {
        BaseActivity activity = (BaseActivity) fragment.getActivity();
        ensureCacheExistence(activity);

        if (!cachedFragmentInjections.containsKey(fragment)) {
            List<Object> injectedObjects = new ArrayList<>(cachedActivityInjections.get(activity));
            if (fragmentInjectionSource != null) {
                injectedObjects.addAll(fragmentInjectionSource.createFor(fragment));
            }
            cachedFragmentInjections.put(fragment, injectedObjects);
        }
    }

    private <T> void injectImpl(T target, List<Object> injectedObjects) {
        Set<Field> allFields = new HashSet<>();
        Class c = target.getClass();
        while (c != null) {
            allFields.addAll(Arrays.asList(c.getDeclaredFields()));
            c = c.getSuperclass();
        }
        for (Field field : allFields) {
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
                    "Couldn't find object to inject into field! Field: %s, type: %s, objects: %s",
                    field.getName(),
                    field.getType().getName(),
                    builder.toString()));
        }
    }
}
