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
package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.EventSchedulerLogger;
import nl.stokpop.eventscheduler.api.TestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class EventProvider implements EventBroadcaster {

    private final EventSchedulerLogger logger;

    private final List<Event> events;

    EventProvider(List<Event> events, EventSchedulerLogger logger) {
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.logger = logger;
    }

    public static EventProvider createInstanceWithEventsFromClasspath(EventSchedulerLogger logger) {
        return createInstanceWithEventsFromClasspath(logger, null);
    }

    public static EventProvider createInstanceWithEventsFromClasspath(EventSchedulerLogger logger, ClassLoader classLoader) {
        ServiceLoader<Event> eventLoader = classLoader == null
                ? ServiceLoader.load(Event.class)
                : ServiceLoader.load(Event.class, classLoader);
        // java 9+: List<PerfanaEvent> events = perfanaEventLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        List<Event> events = new ArrayList<>();
        for (Event event : eventLoader) {
            events.add(event);
        }
        return new EventProvider(events, logger);
    }

    @Override
    public void broadcastBeforeTest(TestContext context, EventSchedulerProperties properties) {
        logger.info("broadcast before test event");
        events.forEach(catchExceptionWrapper(event -> event.beforeTest(context, properties.get(event))));
    }

    @Override
    public void broadcastAfterTest(TestContext context, EventSchedulerProperties properties) {
        logger.info("broadcast after test event");
        events.forEach(catchExceptionWrapper(event -> event.afterTest(context, properties.get(event))));
    }
    
    @Override
    public void broadcastKeepAlive(TestContext context, EventSchedulerProperties properties) {
        logger.debug("broadcast keep alive event");
        events.forEach(catchExceptionWrapper(event -> event.keepAlive(context, properties.get(event))));
    }

    @Override
    public void broadcastAbortTest(TestContext context, EventSchedulerProperties properties) {
        logger.debug("broadcast abort test event");
        events.forEach(catchExceptionWrapper(event -> event.abortTest(context, properties.get(event))));
    }

    @Override
    public void broadcastCustomEvent(TestContext context, EventSchedulerProperties properties, ScheduleEvent scheduleEvent) {
        logger.info("broadcast " + scheduleEvent.getName() + " custom event");
        events.forEach(catchExceptionWrapper(event -> event.customEvent(context, properties.get(event), scheduleEvent)));
    }

    @Override
    public void broadcastCheckResults(TestContext context, EventSchedulerProperties properties) {
        logger.info("broadcast check test");
        events.forEach(catchExceptionWrapper(event -> event.checkTest(context, properties.get(event))));
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
                    System.out.println("(note: better provide a logger): " + message);
                }
            }
        };
    }

}
