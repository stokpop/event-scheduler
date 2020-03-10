package nl.stokpop.eventscheduler.exception.handler;

import nl.stokpop.eventscheduler.api.SchedulerExceptionType;

/**
 * An event can throw AbortSchedulerException to stop the
 * scheduler and trigger the registered abort handlers.
 *
 * Use in case of unrecoverable problems.
 */
public class AbortSchedulerException extends SchedulerHandlerException {
    public AbortSchedulerException(String message) {
        super(message);
    }

    @Override
    public SchedulerExceptionType getExceptionType() {
        return SchedulerExceptionType.ABORT;
    }

}
