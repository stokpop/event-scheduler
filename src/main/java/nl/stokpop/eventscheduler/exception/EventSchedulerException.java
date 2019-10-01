package nl.stokpop.eventscheduler.exception;

public class EventSchedulerException extends Exception {
    
    public EventSchedulerException(final String message) {
        super(message);
    }

    public EventSchedulerException(final String message, final Exception e) {
        super(message, e);
    }
}
