package korablique.recipecalculator.base;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides current time primarily for easier testing.
 */
public interface TimeProvider {
    DateTime now();
    DateTime nowUtc();
}
