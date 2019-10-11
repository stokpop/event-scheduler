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

import nl.stokpop.eventscheduler.api.EventSchedulerLogger;
import nl.stokpop.eventscheduler.event.EventGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class EventGeneratorProvider {

    private Map<String, EventGenerator> generators;
    private EventSchedulerLogger logger;

    EventGeneratorProvider(Map<String, EventGenerator> generators, EventSchedulerLogger logger) {
        this.generators = Collections.unmodifiableMap(new HashMap<>(generators));
        this.logger = logger;
    }

    public static EventGeneratorProvider createInstanceFromClasspath(EventSchedulerLogger logger) {
        return createInstanceFromClasspath(logger, null);
    }

    public static EventGeneratorProvider createInstanceFromClasspath(EventSchedulerLogger logger, ClassLoader classLoader) {
        ServiceLoader<EventGenerator> generatorLoader = classLoader == null
                ? ServiceLoader.load(EventGenerator.class)
                : ServiceLoader.load(EventGenerator.class, classLoader);
        // java 9+: List<Event> generators = generatorLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        Map<String, EventGenerator> generators = new HashMap<>();
        for (EventGenerator generator : generatorLoader) {
            String generatorName = generator.getClass().getName();
            logger.info("registering EventScheduleGenerator: " + generatorName);
            generators.put(generatorName, generator);
        }
        return new EventGeneratorProvider(generators, logger);
    }

    public EventGenerator find(String generatorClassname) {
        return generators.get(generatorClassname);
    }
}
