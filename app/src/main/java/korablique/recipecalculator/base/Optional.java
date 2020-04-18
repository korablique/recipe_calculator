package korablique.recipecalculator.base;

import androidx.annotation.Nullable;

import java.util.NoSuchElementException;

/**
 * Частичная копия {@link java.util.Optional}, который недоступен для нашей версии API.
 */
public class Optional<T> {
    private final T val;

    private Optional(@Nullable T val) {
        this.val = val;
    }

    /**
     * Использование: {@code}Optional<String> os = Optional.of("asd");{@code}
     */
    public static <T> Optional<T> of(T val) {
        if (val == null) {
            throw new IllegalArgumentException("Val must be set");
        }
        return new Optional<>(val);
    }

    /**
     * Использование: {@code}Optional<String> os = Optional.ofNullable("asd");{@code}
     */
    public static <T> Optional<T> ofNullable(@Nullable T val) {
        return new Optional<>(val);
    }

    /**
     * Использование: {@code}Optional<String> os = Optional.empty();{@code}
     */
    public static <T> Optional<T> empty() {
        return new Optional<>(null);
    }

    public boolean isPresent() {
        return this.val != null;
    }

    public T get() {
        if (val == null) {
            throw new NoSuchElementException("No value present");
        }
        return val;
    }
}
