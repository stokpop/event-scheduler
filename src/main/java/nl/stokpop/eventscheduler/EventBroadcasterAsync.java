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
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.exception.handler.SchedulerHandlerException;
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Calls all events in an async manner to avoid the main broadcast thread
 * from being blocked.
 */
public class EventBroadcasterAsync implements EventBroadcaster {

    protected static final int ALL_CALLS_TIME_OUT_SECONDS = 300;
    private final ExecutorService executor;
    private final List<Event> events;
    private final EventLogger logger;

    EventBroadcasterAsync(Collection<Event> events, EventLogger logger, ExecutorService executor) {
        this.events = events == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(events));
        this.logger = logger == null ? EventLoggerDevNull.INSTANCE : logger;
        this.executor = executor == null ? Executors.newCachedThreadPool() : executor;
    }

    public EventBroadcasterAsync(Collection<Event> events, EventLogger logger) {
        this(events, logger, null);
    }

    public EventBroadcasterAsync(Collection<Event> events) {
        this(events, null, null);
    }

    /**
     * The before test calls of all events will run in parallel, but this method will wait for
     * all events to finish before returning.
     *
     * This is to make sure all needed activities before the test have finished before the
     * test will start.
     */
    @Override
    public void broadcastBeforeTest() {
        logger.info("broadcast before test event");

        CompletableFuture<?>[] cfs = this.events.stream()
                .map(e -> CompletableFuture.runAsync(e::beforeTest, executor)
                        .exceptionally(printError(e)))
                .toArray(CompletableFuture<?>[]::new);

        CompletableFuture<Void> allBeforeTests = CompletableFuture.allOf(cfs)
                .exceptionally(t -> {
                    logger.warn("There was an exception calling a before test: " + t.getMessage());
                    return null;
                });

        // block until 'all before' tasks are finished, only then proceed to run test
        try {
            Void aVoid = allBeforeTests.get(ALL_CALLS_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("got interrupt waiting for all 'before test' calls to finish, " +
                    "not all call may have been finished");
        } catch (ExecutionException e) {
            throw new EventSchedulerRuntimeException(
                    "waiting for all 'before test' calls failed", e);
        } catch (TimeoutException e) {
            logger.warn("waited for " + ALL_CALLS_TIME_OUT_SECONDS + " seconds, got timeout waiting, " +
                    "'before test' tasks might still be running?");
        }
    }

    /**
     * The after test calls of all events will run in parallel, but this method will wait for
     * all events to finish before returning.
     *
     * This is to make sure all needed activities after the test have finished before the
     * test will finish.
     */
    @Override
    public void broadcastAfterTest() {
        logger.info("broadcast after test event");
        
        Stream<CompletableFuture<Void>> cfs = this.events.stream()
                .map(e -> CompletableFuture.runAsync(e::afterTest, executor)
                        .exceptionally(printError(e)));

        CompletableFuture<Void> allAfterTests = CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
                .exceptionally(t -> {
                    logger.warn("There was an exception calling a before test: " + t.getMessage());
                    return null;
                });

        // block until all 'after tests' tasks are finished, only then proceed to run test
        try {
            Void aVoid = allAfterTests.get(ALL_CALLS_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("got interrupt waiting for all 'after test' calls to finish, " +
                    "not all call may have been finished");
        } catch (ExecutionException e) {
            throw new EventSchedulerRuntimeException(
                    "waiting for all 'after test' calls failed", e);
        } catch (TimeoutException e) {
            logger.warn("waited for " + ALL_CALLS_TIME_OUT_SECONDS + " seconds, got timeout waiting, " +
                    "'after test' tasks might still be running?");
        }
    }

    @Override
    public void broadcastKeepAlive() {
        logger.debug("broadcast keep alive event");

        Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

        Stream<CompletableFuture<Void>> cfs = this.events.stream()
                .map(e -> CompletableFuture.runAsync(e::keepAlive, executor)
                        .exceptionally(printError(e, exceptions)));

        CompletableFuture<Void> allKeepAlives = CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new));

        // block until all 'keep alive' tasks are finished, then check if KillSwitchException is present
        try {
            Void aVoid = allKeepAlives.get(ALL_CALLS_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("got interrupt waiting for all 'keep alive' calls to finish, " +
                    "not all call may have been finished");
        } catch (ExecutionException e) {
            throw new EventSchedulerRuntimeException(
                    "waiting for all 'keep alive' calls failed", e);
        } catch (TimeoutException e) {
            logger.warn("waited for " + ALL_CALLS_TIME_OUT_SECONDS + " seconds, got timeout waiting for " +
                    "'keep alive' tasks");
        }
        logger.debug("Keep Alive found exceptions: " + exceptions);
        throwAbortOrKillWitchException(exceptions);
    }

    /**
     * Blocks for all abort tasks to be finished to try to make sure they get called
     * before jvm shutdown.
     */
    @Override
    public void broadcastAbortTest() {
        logger.debug("broadcast abort test event");

        Stream<CompletableFuture<Void>> cfs = this.events.stream()
                .map(e -> CompletableFuture.runAsync(e::abortTest, executor)
                        .exceptionally(printError(e)));

        CompletableFuture<Void> allAbortTests = CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
                .exceptionally(t -> {
                    logger.warn("There was an exception calling an abort test: " + t.getMessage());
                    return null;
                });

        // block until all 'abort tests' tasks are finished
        try {
            Void aVoid = allAbortTests.get(ALL_CALLS_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("got interrupt waiting for all 'abort test' calls to finish, " +
                    "not all call may have been finished");
        } catch (ExecutionException e) {
            throw new EventSchedulerRuntimeException(
                    "waiting for all 'abort test' calls failed", e);
        } catch (TimeoutException e) {
            logger.warn("waited for " + ALL_CALLS_TIME_OUT_SECONDS + " seconds, got timeout waiting, " +
                    "'abort test' tasks might still be running?");
        }
    }

    @Override
    public void broadcastCustomEvent(CustomEvent scheduleEvent) {
        logger.info("broadcast " + scheduleEvent.getName() + " custom event");
        this.events.forEach(e -> CompletableFuture.runAsync(() -> e.customEvent(scheduleEvent), executor)
                .exceptionally(printError(e)));
    }

    @Override
    public List<EventCheck> broadcastCheck() {
        logger.info("broadcast check test");

        List<CompletableFuture<EventCheck>> eventChecks = events.stream()
                .map(e -> CompletableFuture.supplyAsync(e::check, executor).exceptionally(getFailureEventCheck(e)))
                .collect(Collectors.toList());

        CompletableFuture<?>[] cfs = eventChecks.toArray(new CompletableFuture<?>[0]);

        CompletableFuture<Void> allEventChecks = CompletableFuture.allOf(cfs)
                .exceptionally(t -> {
                    throw new EventSchedulerRuntimeException(
                            "There was an exception getting an event check: " + t.getMessage()); } );

        CompletableFuture<List<EventCheck>> listCompletableFuture = allEventChecks.thenApply(future ->
                eventChecks.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        try {
            return listCompletableFuture.get(ALL_CALLS_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EventSchedulerRuntimeException("get event checks error", e);
        }

    }

    @Override
    public void shutdownAndWaitAllTasksDone(long timeoutSeconds) {
        logger.info("shutdown broadcaster, waiting up to " + timeoutSeconds + " seconds for tasks to finish");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            logger.warn("forced shutdown broadcaster, some tasks might not have been finished");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("shutdown broadcaster done.");
    }

    private Function<Throwable, EventCheck> getFailureEventCheck(Event e) {
        return t -> {
            EventCheck eventCheck = new EventCheck(e.getName(), e.getClass().getSimpleName(), EventStatus.FAILURE, "Failed to produce an event check! " + t.getMessage());
            logger.error("Error during check: " + eventCheck, t);
            return eventCheck;
        };
    }

    private Function<Throwable, Void> printError(Event e, Queue<Throwable> errors) {
        return t -> {
            // t is CompletionException, so get inner cause
            Throwable cause = t.getCause();
            if (cause instanceof SchedulerHandlerException) {
                logger.debug("SchedulerHandler " + ((SchedulerHandlerException)cause).getExceptionType() + " requested from event '" + e.getName() + "'");
            }
            else {
                logger.error("Event failure in '" + e.getName() + "'", cause);
            }
            if (errors != null) {
                errors.add(cause);
            }
            return null;
        };
    }

    private Function<Throwable, Void> printError(Event e) {
        return printError(e, null);
    }

}
