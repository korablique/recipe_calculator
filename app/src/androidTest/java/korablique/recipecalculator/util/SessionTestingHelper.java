package korablique.recipecalculator.util;

import android.app.Instrumentation;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.joda.time.DateTime;

import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.session.SessionController;

public class SessionTestingHelper {
    private final ActivityTestRule<? extends BaseActivity> activityTestRule;
    private final TestingTimeProvider timeProvider;
    private final CurrentActivityProvider currentActivityProvider;
    @Nullable
    private Runnable firstSessionRunnable;
    @Nullable
    private Runnable secondSessionRunnable;

    SessionTestingHelper(
            ActivityTestRule<? extends BaseActivity> activityTestRule,
            TestingTimeProvider timeProvider,
            CurrentActivityProvider currentActivityProvider) {
        this.activityTestRule = activityTestRule;
        this.timeProvider = timeProvider;
        this.currentActivityProvider = currentActivityProvider;
    }

    public static SessionTestingHelper testSessionWith(
            ActivityTestRule<? extends BaseActivity> activityTestRule,
            TestingTimeProvider timeProvider,
            CurrentActivityProvider currentActivityProvider) {
        return new SessionTestingHelper(activityTestRule, timeProvider, currentActivityProvider);
    }

    public SessionTestingHelper withFirstSession(Runnable firstSessionRunnable) {
        this.firstSessionRunnable = firstSessionRunnable;
        return this;
    }

    public SessionTestingHelper withSecondSession(Runnable secondSessionRunnable) {
        this.secondSessionRunnable = secondSessionRunnable;
        return this;
    }

    public void performActivityRecreation() {
        firstSessionRunnable.run();

        // Настройка отдачи времени конца первой сессии
        DateTime firstSessionEndTime = timeProvider.now();
        BaseActivity oldActivity = activityTestRule.getActivity();

        // Настройка отдачи времени начала второй сессии
        DateTime secondSessionStartTime = firstSessionEndTime.plus(SessionController.MAX_SESSION_LENGTH + 1);
        timeProvider.setTimeSource(() -> {
            boolean isOldActivityGone =
                    currentActivityProvider.getCurrentActivity() != null
                        && currentActivityProvider.getCurrentActivity() != oldActivity;
            if (isOldActivityGone) {
                return secondSessionStartTime;
            } else {
                return firstSessionEndTime;
            }
        });

        // Рестарт активити
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> activityTestRule.getActivity().recreate());

        secondSessionRunnable.run();
    }

    public void performActivityStopAndStart() {
        firstSessionRunnable.run();

        // Настройка отдачи времени конца первой сессии
        DateTime firstSessionEndTime = timeProvider.now();
        DateTime secondSessionStartTime = firstSessionEndTime.plus(SessionController.MAX_SESSION_LENGTH + 1);

        // Уход активити в фон
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            instrumentation.callActivityOnPause(activityTestRule.getActivity());
            instrumentation.callActivityOnStop(activityTestRule.getActivity());
        });

        // Настройка отдачи времени начала второй сессии
        timeProvider.setTime(secondSessionStartTime);

        // Выход активити из фона
        instrumentation.runOnMainSync(() -> {
            instrumentation.callActivityOnStart(activityTestRule.getActivity());
            instrumentation.callActivityOnResume(activityTestRule.getActivity());
        });

        secondSessionRunnable.run();
    }
}
