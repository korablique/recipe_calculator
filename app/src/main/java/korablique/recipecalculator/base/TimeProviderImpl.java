package korablique.recipecalculator.base;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimeProviderImpl implements TimeProvider {
    @Override
    public DateTime now() {
        return DateTime.now();
    }

    @Override
    public DateTime nowUtc() {
        return DateTime.now(DateTimeZone.UTC);
    }
}
