/*
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class EventScheduler {

    private final EventLogger logger;

    private final String name;

    private final EventSchedulerSettings settings;

    private final boolean checkResultsEnabled;

    private final EventBroadcaster broadcaster;

    private final List<CustomEvent> scheduleEvents;
    private volatile SchedulerExceptionHandler schedulerExceptionHandler;

    private final EventSchedulerEngine eventSchedulerEngine;

    private final AtomicBoolean isSessionActive = new AtomicBoolean(false);

    EventScheduler(String name,
                   EventSchedulerSettings settings,
                   boolean checkResultsEnabled,
                   EventBroadcaster broadcaster,
                   List<CustomEvent> scheduleEvents,
                   EventLogger logger,
                   EventSchedulerEngine eventSchedulerEngine,
                   SchedulerExceptionHandler schedulerExceptionHandler) {
        this.name = name;
        this.settings = settings;
        this.checkResultsEnabled = checkResultsEnabled;
        this.broadcaster = broadcaster;
        this.scheduleEvents = scheduleEvents;
        this.logger = logger;
        this.eventSchedulerEngine = eventSchedulerEngine;
        this.schedulerExceptionHandler = schedulerExceptionHandler;
    }

    public void addKillSwitch(SchedulerExceptionHandler schedulerExceptionHandler) {
        this.schedulerExceptionHandler = schedulerExceptionHandler;
    }
    /**
     * Start a test session.
     */
    public void startSession() {
        boolean wasInActive = isSessionActive.compareAndSet(false, true);
        if (!wasInActive) {
            logger.warn("unexpected call to start session, session was active already, ignore call!");
        }
        else {
            logger.info("start test session");

            broadcaster.broadcastBeforeTest();

            eventSchedulerEngine.startKeepAliveThread(name, settings, broadcaster, schedulerExceptionHandler);
            eventSchedulerEngine.startCustomEventScheduler(scheduleEvents, broadcaster);
        }
    }

    /**
     * Stop a test session.
     */
    public void stopSession() {
        boolean wasActive = isSessionActive.compareAndSet(true, false);

        if (!wasActive) {
            logger.warn("unexpected call to stop session, session was inactive already, ignoring call: please debug");
        }
        else {
            logger.info("stop test session.");

            eventSchedulerEngine.shutdownThreadsNow();

            broadcaster.broadcastAfterTest();

            logger.info("all broadcasts for stop test session are done");
        }
    }

    /**
     * @return true when stop or abort has been called.
     */
    public boolean isSessionStopped() {
        return !isSessionActive.get();
    }

    /**
     * Call to abort this test run.
     */
    public void abortSession() {
        boolean wasActive = isSessionActive.compareAndSet(true, false);

        if (!wasActive) {
            logger.warn("unexpected call to abort session, session was inactive already, ignoring call: please debug");
        }
        else {
            logger.info("test session abort called");

            eventSchedulerEngine.shutdownThreadsNow();

            broadcaster.broadcastAbortTest();
        }
    }

    /**
     * Call to check results of this test run. Catch the exception to do something useful.
     * @throws EventCheckFailureException when there are events that report failures
     */
    public void checkResults() throws EventCheckFailureException {
        logger.info("check results called");

        List<EventCheck> eventChecks = broadcaster.broadcastCheck();

        logger.debug("event checks: " + eventChecks);

        boolean success = eventChecks.stream().allMatch(e -> e.getEventStatus() != EventStatus.FAILURE);

        logger.debug("checked " + eventChecks.size() + " event checks, all success: " + success);

        if (!success) {
            String failureMessage = eventChecks.stream()
                    .filter(e -> e.getEventStatus() == EventStatus.FAILURE)
                    .map(e -> String.format("class: '%s' eventId: '%s' message: '%s'", e.getEventClassName(), e.getEventId(), e.getMessage()))
                    .collect(Collectors.joining(", "));
            String message = String.format("event checks with failures found: [%s]", failureMessage);
            if (checkResultsEnabled) {
                logger.info("one or more event checks reported a failure: " + message);
                throw new EventCheckFailureException(message);
            }
            else {
                logger.warn("checkResultsEnabled is false, not throwing EventCheckFailureException with message: " + message);
            }
        }
    }

    @Override
    public String toString() {
        return "EventScheduler [testRunId:" + name + "]";
    }
}
