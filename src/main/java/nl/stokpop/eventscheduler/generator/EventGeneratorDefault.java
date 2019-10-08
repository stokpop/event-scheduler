package nl.stokpop.eventscheduler.generator;

import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.event.EventGenerator;
import nl.stokpop.eventscheduler.event.ScheduleEvent;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EventGeneratorDefault implements EventGenerator {

    private static final String EVENT_SCHEDULE_TAG = "eventSchedule";

    @Override
    public List<ScheduleEvent> generate(TestContext context, EventGeneratorProperties properties) {
        return createTestEvents(properties.getProperty(EVENT_SCHEDULE_TAG));
    }

    public List<ScheduleEvent> createTestEvents(String eventsAsString) {
        if (eventsAsString != null) {
            BufferedReader eventReader = new BufferedReader(new StringReader(eventsAsString));
            List<String> events = eventReader.lines()
                    .map(String::trim)
                    .filter(e -> !e.isEmpty())
                    .collect(Collectors.toList());
            return parseScheduleEvents(events);
        }
        else {
            return Collections.emptyList();
        }
    }

    private List<ScheduleEvent> parseScheduleEvents(List<String> eventSchedule) {
        return eventSchedule.stream()
                .map(ScheduleEvent::createFromLine)
                .collect(Collectors.toList());
    }
}
