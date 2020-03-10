package nl.stokpop.eventscheduler.exception;

/**
 * An event can throw AbortSchedulerException to stop the
 * scheduler and trigger the registered abort handlers.
 *
 * Use in case of unrecoverable problems.
 */
public class AbortSchedulerException extends RuntimeException {
    public AbortSchedulerException(String message) {
        super(message);
    }
}
