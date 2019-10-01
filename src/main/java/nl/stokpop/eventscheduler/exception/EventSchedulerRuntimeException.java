package nl.stokpop.eventscheduler.exception;

public class EventSchedulerRuntimeException extends RuntimeException {

    public EventSchedulerRuntimeException(final String message) {
        super(message);
    }

    public EventSchedulerRuntimeException(final String message, final Exception e) {
        super(message, e);
    }
}
