package korablique.recipecalculator.util;

import org.joda.time.DateTime;

import korablique.recipecalculator.base.TimeProvider;

/**
 * Time provider that always returns same date.
 */
public class TestingTimeProvider implements TimeProvider {
    @Override
    public DateTime now() {
        return new DateTime(2010, 1, 1, 1, 1);
    }

    @Override
    public DateTime nowUtc() {
        return now();
    }
}
