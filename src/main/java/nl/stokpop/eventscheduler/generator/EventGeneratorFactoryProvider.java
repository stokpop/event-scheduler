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

import nl.stokpop.eventscheduler.api.EventGeneratorFactory;
import nl.stokpop.eventscheduler.api.EventLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class EventGeneratorFactoryProvider {

    private Map<String, EventGeneratorFactory> generatorFactories;
    private EventLogger logger;

    EventGeneratorFactoryProvider(Map<String, EventGeneratorFactory> generatorFactories, EventLogger logger) {
        this.generatorFactories = Collections.unmodifiableMap(new HashMap<>(generatorFactories));
        this.logger = logger;
    }

    public static EventGeneratorFactoryProvider createInstanceFromClasspath(EventLogger logger) {
        return createInstanceFromClasspath(logger, null);
    }

    public static EventGeneratorFactoryProvider createInstanceFromClasspath(EventLogger logger, ClassLoader classLoader) {
        ServiceLoader<EventGeneratorFactory> generatorFactoryLoader = classLoader == null
                ? ServiceLoader.load(EventGeneratorFactory.class)
                : ServiceLoader.load(EventGeneratorFactory.class, classLoader);
        // java 9+: List<EventGeneratorFactory> generatorFactories = generatorFactoryLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        Map<String, EventGeneratorFactory> factories = new HashMap<>();
        for (EventGeneratorFactory generatorFactory : generatorFactoryLoader) {
            String generatorName = generatorFactory.getClass().getName();
            logger.info("registering EventScheduleGeneratorFactory: " + generatorName);
            factories.put(generatorName, generatorFactory);
        }
        return new EventGeneratorFactoryProvider(factories, logger);
    }

    public EventGeneratorFactory find(String generatorClassname) {
        return generatorFactories.get(generatorClassname);
    }
}
