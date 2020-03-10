package nl.stokpop.eventscheduler.exception.handler;

import nl.stokpop.eventscheduler.api.SchedulerExceptionType;

/**
 * Exceptions to be passed outside of the event sandbox.
 * Other exceptions are kept within the event.
 */
public abstract class SchedulerHandlerException extends RuntimeException {
    public SchedulerHandlerException(String message) {
        super(message);
    }

    public abstract SchedulerExceptionType getExceptionType();
}
