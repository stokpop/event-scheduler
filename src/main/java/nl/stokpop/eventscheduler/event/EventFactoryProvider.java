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

import nl.stokpop.eventscheduler.api.EventFactory;
import nl.stokpop.eventscheduler.api.EventLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class EventFactoryProvider {

    private final EventLogger logger;

    private final List<EventFactory> eventFactories;

    EventFactoryProvider(List<EventFactory> eventFactories, EventLogger logger) {
        this.eventFactories = Collections.unmodifiableList(new ArrayList<>(eventFactories));
        this.logger = logger;
    }

    public static EventFactoryProvider createInstanceWithEventsFromClasspath(EventLogger logger) {
        return createInstanceWithEventsFromClasspath(logger, null);
    }

    public static EventFactoryProvider createInstanceWithEventsFromClasspath(EventLogger logger, ClassLoader classLoader) {
        ServiceLoader<EventFactory> eventFactoryLoader = classLoader == null
                ? ServiceLoader.load(EventFactory.class)
                : ServiceLoader.load(EventFactory.class, classLoader);
        // java 9+: List<EventFactory> eventFactories = eventFactoryLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        List<EventFactory> eventFactories = new ArrayList<>();
        for (EventFactory eventFactory : eventFactoryLoader) {
            eventFactories.add(eventFactory);
        }
        return new EventFactoryProvider(eventFactories, logger);
    }

}
