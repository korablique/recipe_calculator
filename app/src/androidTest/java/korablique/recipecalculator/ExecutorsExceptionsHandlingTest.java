package korablique.recipecalculator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import io.reactivex.plugins.RxJavaPlugins;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutorImpl;
import korablique.recipecalculator.base.executors.Executor;
import korablique.recipecalculator.base.executors.MainThreadExecutorImpl;
import korablique.recipecalculator.database.DatabaseThreadExecutorImpl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExecutorsExceptionsHandlingTest {
    @Nullable
    private Thread.UncaughtExceptionHandler realUncaughtExceptionsHandler;
    private List<Throwable> uncaughtExceptions = new ArrayList<>();

    @Before
    public void setUp() {
        // We are going to test whether uncaught exceptions thrown from executors cause
        // the app to crash.
        // But we can't let the app actually crash - all tests would fail.
        // Instead, we set uncaught exceptions handlers:

        // Set a new default Java uncaught exceptions handler.
        Assert.assertNull(realUncaughtExceptionsHandler);
        realUncaughtExceptionsHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            uncaughtExceptions.add(throwable);
        });

        // Set a new global Rx uncaught errors handler.
        RxJavaPlugins.setErrorHandler((throwable) -> {
            uncaughtExceptions.add(throwable);
        });
    }

    @After
    public void tearDown() {
        // Reset Java uncaught exceptions handler to the real one.
        Assert.assertNotNull(realUncaughtExceptionsHandler);
        Thread.setDefaultUncaughtExceptionHandler(realUncaughtExceptionsHandler);
        realUncaughtExceptionsHandler = null;

        // Reset Rx uncaught exceptions handler.
        RxJavaPlugins.reset();

        // Clear uncaught exceptions.
        uncaughtExceptions.clear();
    }

    @Test
    public void databaseThreadExecutorTerminateAppOnExceptions() {
        testExecutor(new DatabaseThreadExecutorImpl());
    }

    @Test
    public void computationThreadsExecutorTerminateAppOnExceptions() {
        testExecutor(new ComputationThreadsExecutorImpl());
    }

    @Test
    public void mainThreadExecutorTerminateAppOnExceptions() {
        testExecutor(new MainThreadExecutorImpl());
    }

    private void testExecutor(Executor executor) {
        // Let's create exception then throw it from the given executor.
        RuntimeException exception = new RuntimeException("my exception");
        executor.execute(() -> {
            throw exception;
        });

        // Let's wait for the exception to be detected.
        long timeout = 1000;
        long startTime = System.currentTimeMillis();
        // Spinlock!
        while (System.currentTimeMillis() - startTime < timeout
                && !hasUncaughtException(exception));

        Assert.assertTrue(hasUncaughtException(exception));
    }

    private boolean hasUncaughtException(Throwable exception) {
        // Look for the given exception in all detected uncaught exceptions
        // and all their causes.
        for (Throwable uncaughtException : uncaughtExceptions) {
            Throwable currentException = uncaughtException;
            while (currentException != null) {
                if (currentException == exception) {
                    return true;
                }
                currentException = currentException.getCause();
            }
        }
        return false;
    }
}
