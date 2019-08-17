package korablique.recipecalculator.util;

import org.joda.time.DateTime;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.base.Function0arg;
import korablique.recipecalculator.base.TimeProvider;

/**
 * Time provider that always returns same date.
 */
public class TestingTimeProvider implements TimeProvider {
    public static class ActualTimeSource {
        private DateTime time;
        public ActualTimeSource() {
            this(new DateTime(2010, 1, 1, 1, 1));
        }
        public ActualTimeSource(DateTime time) {
            this.time = time;
        }
        public DateTime now() {
            return time;
        }
    }
    private ActualTimeSource timeSource = new ActualTimeSource();

    public void setTime(DateTime time) {
        this.timeSource = new ActualTimeSource(time);
    }

    public void setTimeSource(ActualTimeSource timeSource) {
        this.timeSource = timeSource;
    }

    public void setTimeSource(Function0arg<DateTime> timeSourceFunc) {
        this.timeSource = new ActualTimeSource() {
            @Override
            public DateTime now() {
                return timeSourceFunc.call();
            }
        };
    }

    @Override
    public DateTime now() {
        return timeSource.now();
    }

    @Override
    public DateTime nowUtc() {
        return timeSource.now();
    }
}
