package korablique.recipecalculator.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import korablique.recipecalculator.BroccalcApplication;

@Singleton
@Component(modules = { AndroidInjectionModule.class, BroccalcApplicationModule.class})
public interface BroccalcApplicationComponent extends AndroidInjector<BroccalcApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder context(Context context);
        BroccalcApplicationComponent build();
    }

    void inject(BroccalcApplication app);
}
