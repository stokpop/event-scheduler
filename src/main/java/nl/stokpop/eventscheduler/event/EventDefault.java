package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.*;

public class EventDefault extends EventAdapter {

    EventDefault(String eventName, TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        super(eventName, testContext, eventProperties, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("Before test: " + testContext.getTestRunId());
    }

    @Override
    public EventCheck check() {
        return EventCheck.DEFAULT;
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {

    }
}
