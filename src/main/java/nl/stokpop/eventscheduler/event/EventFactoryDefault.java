package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.EventFactory;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;

public class EventFactoryDefault implements EventFactory {

    @Override
    public Event create(TestContext context, EventProperties properties) {
        return new EventDefault(context, properties, EventLoggerStdOut.INSTANCE);
    }
}
