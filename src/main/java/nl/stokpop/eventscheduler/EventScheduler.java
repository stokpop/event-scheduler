package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.EventSchedulerLogger;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.event.EventBroadcaster;
import nl.stokpop.eventscheduler.event.EventSchedulerProperties;
import nl.stokpop.eventscheduler.event.ScheduleEvent;
import nl.stokpop.eventscheduler.exception.EventSchedulerException;

import java.util.List;

public final class EventScheduler {

    private final EventSchedulerLogger logger;

    private final TestContext context;
    private final EventSchedulerSettings settings;

    private final boolean checkResultsEnabled;

    private final EventBroadcaster broadcaster;
    private final EventSchedulerProperties eventProperties;
    private final List<ScheduleEvent> scheduleEvents;

    private EventSchedulerEngine executorEngine;

    private boolean isSessionStopped = false;

    EventScheduler(TestContext context, EventSchedulerSettings settings,
                   boolean checkResultsEnabled, EventBroadcaster broadcaster,
                   EventSchedulerProperties eventProperties,
                   List<ScheduleEvent> scheduleEvents, EventSchedulerLogger logger) {
        this.context = context;
        this.settings = settings;
        this.checkResultsEnabled = checkResultsEnabled;
        this.eventProperties = eventProperties;
        this.broadcaster = broadcaster;
        this.scheduleEvents = scheduleEvents;
        this.logger = logger;
    }

    /**
     * Start a test session.
     */
    public void startSession() {
        logger.info("Start test session");
        isSessionStopped = false;

        executorEngine = new EventSchedulerEngine(logger);

        broadcaster.broadcastBeforeTest(context, eventProperties);

        executorEngine.startKeepAliveThread(context, settings, broadcaster, eventProperties);
        executorEngine.startCustomEventScheduler(context, scheduleEvents, broadcaster, eventProperties);

    }

    /**
     * Stop a test session.
     * @throws EventSchedulerException when something fails in executing an event
     */
    public void stopSession() throws EventSchedulerException {
        logger.info("Stop test session.");
        isSessionStopped = true;
        
        executorEngine.shutdownThreadsNow();

        broadcaster.broadcastAfterTest(context, eventProperties);

        if (checkResultsEnabled) {
            // TODO: think about how to succeed or fail based on checks.
            broadcaster.broadcastCheckResults(context, eventProperties);
        }

        logger.info("All broadcasts for stop test session are done");
    }

    public boolean isSessionStopped() {
        return isSessionStopped;
    }

    /**
     * Call to abort this test run.
     */
    public void abortSession() {
        logger.warn("Test session abort called.");
        isSessionStopped = true;

        broadcaster.broadcastAbortTest(context, eventProperties);

        executorEngine.shutdownThreadsNow();
    }

    @Override
    public String toString() {
        return String.format("EventScheduler [testRunId:%s testType:%s testEnv:%s]",
                context.getTestRunId(), context.getTestType(), context.getTestEnvironment());
    }
}
