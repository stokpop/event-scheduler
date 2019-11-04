package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.EventCheck;
import nl.stokpop.eventscheduler.api.*;

public class EventDefault implements Event {

    private final EventProperties eventProperties;
    private final TestContext testContext;
    private final EventLogger logger;
    private final String eventName;

    EventDefault(String eventName, TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        this.eventName = eventName;
        this.testContext = testContext;
        this.eventProperties = eventProperties;
        this.logger = logger;
    }

    @Override
    public String getName() {
        return eventName;
    }

    @Override
    public void beforeTest() {
        logger.info("Before test: " + testContext.getTestRunId());
    }

    @Override
    public void afterTest() {

    }

    @Override
    public void keepAlive() {

    }

    @Override
    public void abortTest() {

    }

    @Override
    public EventCheck check() {
        return EventCheck.DEFAULT;
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {

    }
}
