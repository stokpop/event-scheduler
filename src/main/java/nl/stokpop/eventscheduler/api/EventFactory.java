package nl.stokpop.eventscheduler.api;

import nl.stokpop.eventscheduler.event.Event;
import nl.stokpop.eventscheduler.event.EventProperties;

/**
 * Create an EventGenerator based on the given test context and properties.
 */
public interface EventFactory {
    Event create(TestContext context, EventProperties properties);
}
