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
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EventBroadcasterDefault implements EventBroadcaster {

    private final List<Event> events;
    private final EventLogger logger;

    EventBroadcasterDefault(Collection<Event> events, EventLogger logger) {
        this.events = events == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(events));
        this.logger = logger == null ? EventLoggerDevNull.INSTANCE : logger;
    }

    public EventBroadcasterDefault(Collection<Event> events) {
        this(events, null);
    }

    @Override
    public void broadcastBeforeTest() {
        logger.info("broadcast before test event");
        events.forEach(catchExceptionWrapper(Event::beforeTest));
    }

    @Override
    public void broadcastAfterTest() {
        logger.info("broadcast after test event");
        events.forEach(catchExceptionWrapper(Event::afterTest));
    }

    @Override
    public void broadcastKeepAlive() {
        logger.debug("broadcast keep alive event");
        events.forEach(catchExceptionWrapper(Event::keepAlive));
    }

    @Override
    public void broadcastAbortTest() {
        logger.debug("broadcast abort test event");
        events.forEach(catchExceptionWrapper(Event::abortTest));
    }

    @Override
    public void broadcastCustomEvent(CustomEvent scheduleEvent) {
        logger.info("broadcast " + scheduleEvent.getName() + " custom event");
        events.forEach(catchExceptionWrapper(event -> event.customEvent(scheduleEvent)));
    }

    @Override
    public List<EventCheck> broadcastCheck() {
        logger.info("broadcast check test");
        return events.stream().map(Event::check).collect(Collectors.toList());
    }

    @Override
    public void shutdownAndWaitAllTasksDone(long timeoutSeconds) {
        logger.debug("shutdown broadcaster called, is noop in this implementation.");
    }

    /**
     * Make sure events continue, even when exceptions are thrown.
     */
    private Consumer<Event> catchExceptionWrapper(Consumer<Event> consumer) {
        return event -> {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                String message = String.format("exception in event (%s)", event.getName());
                if (logger != null) {
                    logger.error(message, e);
                }
                else {
                    System.err.printf("(note: better provide a logger): %s %s", message, e.getMessage());
                }
            }
        };
    }

}
