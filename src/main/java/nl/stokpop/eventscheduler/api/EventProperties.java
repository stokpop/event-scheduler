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
package nl.stokpop.eventscheduler.api;

import jdk.nashorn.internal.ir.annotations.Immutable;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * The EventProperties is a list of properties to be used by the Event.
 *
 * One required property is the eventFactory property that should contain the fully qualified
 * class name of the factory class for the event.
 *
 * This is an immutable class and makes an unmodifiable copy of the given Map.
 */
@Immutable
public class EventProperties {
    public static final String FACTORY_CLASSNAME_KEY = "eventFactory";

    private Map<String, String> properties;

    public EventProperties(Properties properties) {
        // note: null cannot be added to Properties, so check is not needed
        this(properties.entrySet().stream().collect(Collectors.toMap(x -> x.getKey().toString(), x -> x.getValue().toString())));
    }

    public EventProperties(Map<String,String> props) {
        properties = Collections.unmodifiableMap(new HashMap<>(props));
        if (!properties.containsKey(FACTORY_CLASSNAME_KEY)) {
            throw new EventSchedulerRuntimeException("The " + FACTORY_CLASSNAME_KEY + " property is missing, add it with value the fully" +
                    " qualified class name of the factory for the events.");
        }
    }

    public EventProperties() {
        properties = Collections.unmodifiableMap(Collections.emptyMap());
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public String getPropertyOrDefault(String name, String defaultValue) {
        return properties.getOrDefault(name, defaultValue);
    }

    public String getFactoryClassName() {
        return properties.get(FACTORY_CLASSNAME_KEY);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) { return true; }
        if (obj instanceof EventProperties) {
            return properties.equals(((EventProperties) obj).properties);
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "EventProperties{" +
                "properties=" + properties +
                '}';
    }

}
