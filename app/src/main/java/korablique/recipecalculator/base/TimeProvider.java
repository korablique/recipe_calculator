package korablique.recipecalculator.base;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides current time primarily for easier testing.
 */
@Singleton
public class TimeProvider {
    @Inject
    public TimeProvider() {
    }

    public DateTime now() {
        return DateTime.now();
    }

    public DateTime nowUtc() {
        return DateTime.now(DateTimeZone.UTC);
    }
}
