package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.exception.PerfanaClientRuntimeException;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store event properties per Event implementation class.
 * Uses getClass().getCanonicalName() so inner classes will use '.' instead of '$' as name separator.
 * '$' can not be used in most situation, like in xml element name (e.g. maven pom.xml).
 *
 * Intention is to be threadsafe using ConcurrentHashMaps, not 100% sure it is.
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
            String msg = String.format("Anonymous classes are not allowed for PerfanaEvent, sorry. [%s]", event.getClass());
            throw new PerfanaClientRuntimeException(msg);
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
