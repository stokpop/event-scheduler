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

import java.util.*;
import java.util.stream.Collectors;

public class EventFactoryProvider {

    private final Map<String, EventFactory> eventFactories;

    private EventFactoryProvider(List<EventFactory> eventFactories) {
        Map<String, EventFactory> map = eventFactories.stream()
                .collect(Collectors.toMap(f -> f.getClass().getName(), f -> f));
        this.eventFactories = Collections.unmodifiableMap(map);
    }

    public static EventFactoryProvider createInstanceFromClasspath() {
        return createInstanceFromClasspath(null);
    }

    public static EventFactoryProvider createInstanceFromClasspath(ClassLoader classLoader) {
        ServiceLoader<EventFactory> eventFactoryLoader = classLoader == null
                ? ServiceLoader.load(EventFactory.class)
                : ServiceLoader.load(EventFactory.class, classLoader);
        // java 9+: List<EventFactory> eventFactories = eventFactoryLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        List<EventFactory> eventFactories = new ArrayList<>();
        for (EventFactory eventFactory : eventFactoryLoader) {
            eventFactories.add(eventFactory);
        }
        return new EventFactoryProvider(eventFactories);
    }

    /**
     * Find factory by given class name.
     *
     * @param className the full class name of the factory
     * @return an optional which is empty if the factory for given class name is not present
     */
    public Optional<EventFactory> factoryByClassName(String className) {
        return Optional.ofNullable(eventFactories.get(className));
    }

}
