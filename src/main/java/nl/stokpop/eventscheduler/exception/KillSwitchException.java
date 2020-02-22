package nl.stokpop.eventscheduler.exception;

public class KillSwitchException extends RuntimeException {
    public KillSwitchException(String message) {
        super(message);
    }
}
