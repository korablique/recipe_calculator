package korablique.recipecalculator;

public class WrongThreadException extends RuntimeException {
    public WrongThreadException() {}

    public WrongThreadException(String message) {
        super(message);
    }
}
