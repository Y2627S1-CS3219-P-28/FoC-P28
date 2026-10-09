package sg.edu.nus.foc.credit.error;

public class CreditStreamUnavailableException extends RuntimeException {

    public CreditStreamUnavailableException(Throwable cause) {
        super("Credit balance notifications are temporarily unavailable.", cause);
    }
}
