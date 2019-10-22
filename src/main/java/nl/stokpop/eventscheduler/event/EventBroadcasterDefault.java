package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class EventBroadcasterDefault implements EventBroadcaster {

    private final List<Event> events;
    private final EventLogger logger;

    public EventBroadcasterDefault(Collection<Event> events, EventLogger logger) {
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
    public void broadcastCheckResults() {
        logger.info("broadcast check test");
        events.forEach(catchExceptionWrapper(Event::checkTest));
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
