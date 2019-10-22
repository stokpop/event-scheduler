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

import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store event properties per Event implementation class.
 * Uses getClass().getCanonicalName() so inner classes will use '.' instead of '$' as name separator.
 * '$' can not be used in most situation, like in xml element name (e.g. maven pom.xml).
 *
 * Intent is to be thread safe using ConcurrentHashMaps, not 100% sure it is.
 */
public class EventSchedulerProperties {

    private Map<String, Map<String, String>> eventProperties = new ConcurrentHashMap<>();

    public EventProperties get(Event event) {
        String canonicalName = determineCanonicalName(event);
        Map<String, String> props = eventProperties.getOrDefault(canonicalName, Collections.emptyMap());
        return new EventProperties(props);
    }

    private static String determineCanonicalName(Event event) {
        String canonicalName = event.getClass().getCanonicalName();
        if (canonicalName == null) {
            String msg = String.format("Anonymous classes are not allowed for Even classes, sorry. [%s]", event.getClass());
            throw new EventSchedulerRuntimeException(msg);
        }
        return canonicalName;
    }

    public EventSchedulerProperties put(Event event, String name, String value) {
        String classImplName = determineCanonicalName(event);
        put(classImplName, name, value);
        return this;
    }

    public EventSchedulerProperties put(String eventClassImplName, String name, String value) {
        eventProperties.computeIfAbsent(eventClassImplName, k -> new ConcurrentHashMap<>()).put(name, value);
        return this;
    }

}
