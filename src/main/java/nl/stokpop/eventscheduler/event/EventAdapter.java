package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.TestContext;

/**
 * Adapter class with empty method implementations of the Event interface.
 * Extend this class so you only have to implement the methods that are used.
 *
 * Always provide a proper name for an Event for traceability.
 */
public abstract class EventAdapter implements Event {

    @Override
    public void beforeTest(TestContext context, EventProperties properties) {

    }

    @Override
    public void afterTest(TestContext context, EventProperties properties) {

    }

    @Override
    public void keepAlive(TestContext context, EventProperties properties) {

    }

    @Override
    public void customEvent(TestContext context, EventProperties properties, ScheduleEvent scheduleEvent) {

    }
}
