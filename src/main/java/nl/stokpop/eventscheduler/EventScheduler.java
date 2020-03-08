/*
 * Copyright (C) 2020 Peter Paul Bakker, Stokpop Software Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.*;
import nl.stokpop.eventscheduler.exception.EventCheckFailureException;

import java.util.List;
import java.util.stream.Collectors;

public final class EventScheduler {

    private final EventLogger logger;

    private final TestContext context;
    private final EventSchedulerSettings settings;

    private final boolean checkResultsEnabled;

    private final EventBroadcaster broadcaster;
    private final EventProperties eventProperties;
    private final List<CustomEvent> scheduleEvents;
    private KillSwitchHandler killSwitchHandler;

    private EventSchedulerEngine executorEngine;

    private boolean isSessionStopped = false;

    EventScheduler(TestContext context, EventSchedulerSettings settings,
                   boolean checkResultsEnabled, EventBroadcaster broadcaster,
                   EventProperties eventProperties,
                   List<CustomEvent> scheduleEvents, EventLogger logger, KillSwitchHandler killSwitchHandler) {
        this.context = context;
        this.settings = settings;
        this.checkResultsEnabled = checkResultsEnabled;
        this.eventProperties = eventProperties;
        this.broadcaster = broadcaster;
        this.scheduleEvents = scheduleEvents;
        this.logger = logger;
        this.executorEngine = new EventSchedulerEngine(logger);
        this.killSwitchHandler = killSwitchHandler;
    }

    public void addKillSwitch(KillSwitchHandler killSwitchHandler) {
        this.killSwitchHandler = killSwitchHandler;
    }
    /**
     * Start a test session.
     */
    public void startSession() {
        logger.info("Start test session");
        isSessionStopped = false;
        
        broadcaster.broadcastBeforeTest();

        executorEngine.startKeepAliveThread(context, settings, broadcaster, eventProperties, killSwitchHandler);
        executorEngine.startCustomEventScheduler(context, scheduleEvents, broadcaster, eventProperties);

    }

    /**
     * Stop a test session.
     */
    public void stopSession() {
        logger.info("Stop test session.");
        isSessionStopped = true;

        broadcaster.broadcastAfterTest();

        executorEngine.shutdownThreadsNow();

        logger.info("All broadcasts for stop test session are done");
    }

    /**
     * @return true when stop or abort has been called.
     */
    public boolean isSessionStopped() {
        return isSessionStopped;
    }

    /**
     * Call to abort this test run.
     */
    public void abortSession() {
        logger.info("Test session abort called.");
        isSessionStopped = true;

        broadcaster.broadcastAbortTest();

        executorEngine.shutdownThreadsNow();
    }

    /**
     * Call to check results of this test run. Catch the exception to do something useful.
     * @throws EventCheckFailureException when there are events that report failures
     */
    public void checkResults() throws EventCheckFailureException {
        logger.info("Check results called.");

        List<EventCheck> eventChecks = broadcaster.broadcastCheck();

        boolean success = eventChecks.stream().allMatch(e -> e.getEventStatus() != EventStatus.FAILURE);

        logger.debug("Checked " + eventChecks.size() + " event checks. All success: " + success);

        if (!success) {
            String failureMessage = eventChecks.stream()
                    .filter(e -> e.getEventStatus() == EventStatus.FAILURE)
                    .map(e -> String.format("class: '%s' eventId: '%s' message: '%s'", e.getEventClassName(), e.getEventId(), e.getMessage()))
                    .collect(Collectors.joining(", "));
            String message = String.format("Event checks with failures found: [%s]", failureMessage);
            if (checkResultsEnabled) {
                logger.info("One or more event checks reported a failure: " + message);
                throw new EventCheckFailureException(message);
            }
            else {
                logger.warn("CheckResultsEnabled is false, not throwing EventCheckFailureException with message: " + message);
            }
        }
    }

    @Override
    public String toString() {
        return "EventScheduler [testRunId:" + context.getTestRunId()
            + " testType:" + context.getWorkload()
            + " testEnv:" + context.getEnvironment() + "]";
    }
}
