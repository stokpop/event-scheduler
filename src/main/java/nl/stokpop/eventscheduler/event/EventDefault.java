package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.TestContext;

public class EventDefault implements Event {

    private final EventProperties eventProperties;
    private final TestContext testContext;
    private final EventLogger logger;

    EventDefault(TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        this.testContext = testContext;
        this.eventProperties = eventProperties;
        this.logger = logger;
    }

    @Override
    public String getName() {
        return "DefaultEvent";
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
    public void checkTest() {

    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {

    }
}
