package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.generator.EventGeneratorProperties;

import java.util.List;

/**
 * Create a custom event schedule for events.
 *
 * Implement in your own code, use your own input files and/or logic to generate events.
 */
public interface EventGenerator {
    List<ScheduleEvent> generate(TestContext context, EventGeneratorProperties properties);
}
