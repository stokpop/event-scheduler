package nl.stokpop.eventscheduler.exception;

public class PerfanaClientException extends Exception {
    
    public PerfanaClientException(final String message) {
        super(message);
    }

    public PerfanaClientException(final String message, final Exception e) {
        super(message, e);
    }
}
