package nl.stokpop.eventscheduler.api;

/**
 * Create an EventGenerator based on the given test context and properties.
 */
public interface EventFactory {
    Event create(String eventName, TestContext context, EventProperties properties);
}
