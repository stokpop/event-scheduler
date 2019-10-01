package nl.stokpop.eventscheduler.exception;

public class PerfanaClientRuntimeException extends RuntimeException {

    public PerfanaClientRuntimeException(final String message) {
        super(message);
    }

    public PerfanaClientRuntimeException(final String message, final Exception e) {
        super(message, e);
    }
}
