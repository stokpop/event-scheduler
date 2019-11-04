package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.Event;
import nl.stokpop.eventscheduler.api.EventFactory;
import nl.stokpop.eventscheduler.api.EventProperties;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;

public class EventFactoryDefault implements EventFactory {

    private String name;

    public EventFactoryDefault() {
        this("EventFactoryDefault default constructor instance");
    }

    EventFactoryDefault(String name) {
        this.name = name;
    }

    @Override
    public Event create(String eventName, TestContext context, EventProperties properties) {
        return new EventDefault(eventName, context, properties, EventLoggerStdOut.INSTANCE);
    }

    @Override
    public String toString() {
        return "EventFactoryDefault (" + name + ")";
    }
}
