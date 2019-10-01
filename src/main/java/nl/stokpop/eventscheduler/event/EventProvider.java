package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.PerfanaClientLogger;
import nl.stokpop.eventscheduler.api.TestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class EventProvider implements EventBroadcaster {

    private final PerfanaClientLogger logger;

    private final List<Event> events;

    EventProvider(List<Event> events, PerfanaClientLogger logger) {
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.logger = logger;
    }

    public static EventProvider createInstanceWithEventsFromClasspath(PerfanaClientLogger logger) {
        return createInstanceWithEventsFromClasspath(logger, null);
    }

    public static EventProvider createInstanceWithEventsFromClasspath(PerfanaClientLogger logger, ClassLoader classLoader) {
        ServiceLoader<Event> perfanaEventLoader = classLoader == null
                ? ServiceLoader.load(Event.class)
                : ServiceLoader.load(Event.class, classLoader);
        // java 9+: List<PerfanaEvent> events = perfanaEventLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        List<Event> events = new ArrayList<>();
        for (Event event : perfanaEventLoader) {
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
    public void broadCastKeepAlive(TestContext context, EventSchedulerProperties properties) {
        logger.debug("broadcast keep alive event");
        events.forEach(catchExceptionWrapper(event -> event.keepAlive(context, properties.get(event))));

    }

    @Override
    public void broadcastCustomEvent(TestContext context, EventSchedulerProperties properties, ScheduleEvent scheduleEvent) {
        logger.info("broadcast " + scheduleEvent.getName() + " custom event");
        events.forEach(catchExceptionWrapper(event -> event.customEvent(context, properties.get(event), scheduleEvent)));
    }

    /**
     * Make sure events continue, even when exceptions are thrown.
     */
    private Consumer<Event> catchExceptionWrapper(Consumer<Event> consumer) {
        return event -> {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                String message = String.format("exception in perfana event (%s)", event.getName());
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
