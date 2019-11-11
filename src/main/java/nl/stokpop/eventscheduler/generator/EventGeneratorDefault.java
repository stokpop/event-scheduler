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
package nl.stokpop.eventscheduler.generator;

import nl.stokpop.eventscheduler.api.CustomEvent;
import nl.stokpop.eventscheduler.api.EventGenerator;
import nl.stokpop.eventscheduler.api.EventGeneratorProperties;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EventGeneratorDefault implements EventGenerator {

    public static final String EVENT_SCHEDULE_TAG = "eventSchedule";

    private final EventGeneratorProperties properties;

    EventGeneratorDefault(TestContext context, EventGeneratorProperties properties, EventLogger logger) {
        this.properties = properties;
    }

    @Override
    public List<CustomEvent> generate() {
        return createTestEvents(properties.getProperty(EVENT_SCHEDULE_TAG));
    }

    private List<CustomEvent> createTestEvents(String eventsAsString) {
        if (eventsAsString != null) {
            try {
                try (BufferedReader eventReader = new BufferedReader(new StringReader(eventsAsString))) {
                    List<String> events = eventReader.lines()
                            .map(String::trim)
                            .filter(e -> !e.isEmpty())
                            .filter(e -> !e.startsWith("@"))
                            .collect(Collectors.toList());
                    return parseScheduleEvents(events);
                }
            } catch (IOException e) {
                throw new EventSchedulerRuntimeException("Unable to generate test events");
            }
        }
        else {
            return Collections.emptyList();
        }
    }

    private List<CustomEvent> parseScheduleEvents(List<String> eventSchedule) {
        return eventSchedule.stream()
                .map(CustomEvent::createFromLine)
                .collect(Collectors.toList());
    }
}
