package korablique.recipecalculator.dagger;

import korablique.recipecalculator.TestEnvironmentDetector;

public class InjectorHolder {
    private static Injector theInjector;

    static {
        if (!TestEnvironmentDetector.isInTests()) {
            setInjector(new DefaultInjector());
        }
    }

    private InjectorHolder() {
    }

    public static synchronized Injector getInjector() {
        return theInjector;
    }

    public static synchronized void setInjector(Injector injector) {
        theInjector = injector;
    }
}
