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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The EventProperties is a list of properties to be used by the Event.
 *
 * One required property is the eventFactory property that should contain the fully qualified
 * class name of the factory class for the event.
 *
 * Another optional property is "enabled", default is true.
 * If set to false, the event will not be active.
 *
 * This is an immutable class and makes an unmodifiable copy of the given Map.
 */
@Immutable
public class EventProperties {
    public static final String PROP_FACTORY_CLASSNAME = "eventFactory";
    public static final String PROP_EVENT_ENABLED = "enabled";

    private static final List<String> DEFAULT_PROPERTIES;

    static {
        List<String> props = new ArrayList<>();
        props.add(PROP_FACTORY_CLASSNAME);
        props.add(PROP_EVENT_ENABLED);
        DEFAULT_PROPERTIES = Collections.unmodifiableList(props);
    }

    private Map<String, String> properties;

    public EventProperties(Properties properties) {
        // note: null cannot be added to Properties, so check is not needed
        this(properties.entrySet().stream().collect(Collectors.toMap(x -> x.getKey().toString(), x -> x.getValue().toString())));
    }

    public EventProperties(Map<String,String> props) {
        properties = Collections.unmodifiableMap(new HashMap<>(props));
        if (!properties.containsKey(PROP_FACTORY_CLASSNAME)) {
            throw new EventSchedulerRuntimeException("The " + PROP_FACTORY_CLASSNAME + " property is missing, add it with value the fully" +
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
        return properties.get(PROP_FACTORY_CLASSNAME);
    }

    public boolean isEventEnabled() { return Boolean.parseBoolean(properties.getOrDefault(PROP_EVENT_ENABLED, "true")); }

    public void checkUnknownProperties(Collection<String> allowedProperties,
                                       BiConsumer<String, String> unknownPropertyAction) {
        properties.entrySet().stream()
                .filter(e -> !DEFAULT_PROPERTIES.contains(e.getKey()))
                .filter(e -> !allowedProperties.contains(e.getKey()))
                .forEach(e -> unknownPropertyAction.accept(e.getKey(), e.getValue()));
    }

    public boolean containsUnknownProperties(
            Collection<String> allowedProperties) {
        return properties.values().stream()
                .filter(e -> !DEFAULT_PROPERTIES.contains(e))
                .anyMatch(e -> !allowedProperties.contains(e));
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
