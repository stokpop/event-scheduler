/*
 * Copyright (C) 2019 Peter Paul Bakker, Stokpop Software Solutions
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

import nl.stokpop.eventscheduler.api.CustomEvent;
import nl.stokpop.eventscheduler.api.Event;
import nl.stokpop.eventscheduler.api.EventCheck;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.EventStatus;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calls all events in an async manner to avoid the main broadcast thread
 * from being blocked.
 */
public class EventBroadcasterAsync implements EventBroadcaster {

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

    @Override
    public void broadcastBeforeTest() {
        logger.info("broadcast before test event");
        this.events.forEach(e -> CompletableFuture.runAsync(e::beforeTest, executor)
                        .exceptionally(getWrappedError(e)));
    }

    private Function<Throwable, Void> getWrappedError(Event e) {
        return t -> { logger.error("Event failure in " + e.getName(), t); throw new RuntimeException("Wrapped Error", t); };
    }

    @Override
    public void broadcastAfterTest() {
        logger.info("broadcast after test event");
        this.events.forEach(e -> CompletableFuture.runAsync(e::afterTest, executor)
                .exceptionally(getWrappedError(e)));
    }

    @Override
    public void broadcastKeepAlive() {
        logger.debug("broadcast keep alive event");
        this.events.forEach(e -> CompletableFuture.runAsync(e::keepAlive, executor)
                .exceptionally(getWrappedError(e)));
    }

    @Override
    public void broadcastAbortTest() {
        logger.debug("broadcast abort test event");
        this.events.forEach(e -> CompletableFuture.runAsync(e::abortTest, executor)
                .exceptionally(getWrappedError(e)));
    }

    @Override
    public void broadcastCustomEvent(CustomEvent scheduleEvent) {
        logger.info("broadcast " + scheduleEvent.getName() + " custom event");
        this.events.forEach(e -> CompletableFuture.runAsync(() -> e.customEvent(scheduleEvent), executor)
                .exceptionally(getWrappedError(e)));
    }

    @Override
    public List<EventCheck> broadcastCheck() {
        logger.info("broadcast check test");
        
        List<CompletableFuture<EventCheck>> eventChecks = events.stream()
                .map(e -> CompletableFuture.supplyAsync(e::check, executor).exceptionally(getFailureEventCheck(e)))
                .collect(Collectors.toList());

        CompletableFuture[] cfs = eventChecks.toArray(new CompletableFuture[0]);

        CompletableFuture<Void> allEventChecks = CompletableFuture.allOf(cfs)
                .exceptionally(t -> { throw new EventSchedulerRuntimeException("There was an exception getting an event check: " + t.getMessage()); } );

        CompletableFuture<List<EventCheck>> listCompletableFuture = allEventChecks.thenApply(future ->
                eventChecks.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        try {
            return listCompletableFuture.get(120, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EventSchedulerRuntimeException("get event checks error", e);
        }

    }

    private Function<Throwable, EventCheck> getFailureEventCheck(Event e) {
        return t -> {
            EventCheck eventCheck = new EventCheck(e.getName(), e.getClass().getSimpleName(), EventStatus.FAILURE, "Failed to produce an event check! " + t.getMessage());
            logger.error("Error during check: " + eventCheck, t);
            return eventCheck;
        };
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

}
