package korablique.recipecalculator.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimeUtils {
    public static long currentMillis() {
        return DateTime.now(DateTimeZone.UTC).getMillis();
    }
}
