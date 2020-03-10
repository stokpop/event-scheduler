/*
 * Copyright (C) 2020 Peter Paul Bakker, Stokpop Software Solutions
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
package nl.stokpop.eventscheduler;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import nl.stokpop.eventscheduler.api.*;
import nl.stokpop.eventscheduler.event.EventFactoryProvider;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.generator.EventGeneratorDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryProvider;
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;
import nl.stokpop.eventscheduler.log.EventLoggerWithName;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder: intended to be used in one thread for construction and then to be discarded.
 */
@NotThreadSafe
public class EventSchedulerBuilder {
    
    private final Set<EventInfo> eventInfos = new HashSet<>();

    private TestContext testContext;

    private EventSchedulerSettings eventSchedulerSettings;

    private boolean assertResultsEnabled = false;

    private EventProperties eventProperties = new EventProperties();

    private String customEventsText = "";

    private EventLogger logger = EventLoggerDevNull.INSTANCE;

    private EventFactoryProvider eventFactoryProvider;

    private EventBroadcasterFactory eventBroadcasterFactory;

    private SchedulerExceptionHandler schedulerExceptionHandler;

    public EventSchedulerBuilder setSchedulerExceptionHandler(SchedulerExceptionHandler callback) {
        this.schedulerExceptionHandler = callback;
        return this;
    }
    public EventSchedulerBuilder setTestContext(TestContext context) {
        this.testContext = context;
        return this;
    }

    public EventSchedulerBuilder setLogger(EventLogger logger) {
        this.logger = logger;
        return this;
    }

    public EventSchedulerBuilder setEventSchedulerSettings(EventSchedulerSettings settings) {
        this.eventSchedulerSettings = settings;
        return this;
    }

    public EventSchedulerBuilder setAssertResultsEnabled(boolean assertResultsEnabled) {
        this.assertResultsEnabled = assertResultsEnabled;
        return this;
    }

    public EventScheduler build() {
        return build(null);
    }

    /**
     * Clients can use this build method to define a different classloader.
     *
     * By default the classloader from the current thread is used to load event providers and related resources.
     *
     * For example in a Gradle plugin the thread classpath is limited to plugin classes,
     * and does not contain classes from the project context, such as the custom event providers used in the project.
     *
     * @param classLoader the class loader, if null the default classloader of Java's ServiceLoader will be used
     * @return a new EventScheduler
     */
    public EventScheduler build(ClassLoader classLoader) {
        
        if (testContext == null) {
            throw new EventSchedulerRuntimeException("TestContext must be set, it is null.");
        }

        EventSchedulerSettings myEventSchedulerSettings = eventSchedulerSettings == null
                ? new EventSchedulerSettingsBuilder().build()
                : eventSchedulerSettings;
        
        List<CustomEvent> customEvents =
                generateCustomEventSchedule(testContext, customEventsText, logger, classLoader);

        EventFactoryProvider provider = eventFactoryProvider == null
                ? EventFactoryProvider.createInstanceFromClasspath(classLoader)
                : eventFactoryProvider;

        eventInfos.stream()
                .filter(e -> !e.getEventProperties().isEventEnabled())
                .forEach(e -> logger.info("Event disabled: " + e.eventName));

        List<Event> events = eventInfos.stream()
                .filter(e -> e.getEventProperties().isEventEnabled())
                .map(p -> createEvent(provider, p, testContext))
                .collect(Collectors.toList());

        EventBroadcasterFactory broadcasterFactory = eventBroadcasterFactory == null
                ? EventBroadcasterAsync::new
                : eventBroadcasterFactory;

        EventBroadcaster broadcaster = broadcasterFactory.create(events, logger);

        return new EventScheduler(testContext, myEventSchedulerSettings, assertResultsEnabled,
                broadcaster, eventProperties, customEvents, logger, schedulerExceptionHandler);
    }

    private Event createEvent(EventFactoryProvider provider, EventInfo eventInfo, TestContext testContext) {
        EventProperties eventProperties = eventInfo.getEventProperties();
        String factoryClassName = eventProperties.getFactoryClassName();
        String eventName = eventInfo.getEventName();
        EventLogger eventLogger = new EventLoggerWithName(eventName, removeFactoryPostfix(factoryClassName), logger);

        Event event = provider.factoryByClassName(factoryClassName)
                .orElseThrow(() -> new RuntimeException(factoryClassName + " not found on classpath"))
                .create(eventName, testContext, eventProperties, eventLogger);

        Collection<String> allowedProperties = event.allowedProperties();
        eventProperties.checkUnknownProperties(allowedProperties,
                (key, value) -> eventLogger.warn(String.format("unknown property found: '%s' with value: '%s'. Choose from: %s", key, value, allowedProperties)));

        if (logger.isDebugEnabled()) {
            logger.debug("allowed properties: " + event.allowedProperties());
            logger.debug("allowed events    : " + event.allowedCustomEvents());
        }

        return event;
    }

    private String removeFactoryPostfix(String factoryClassName) {
        int index = factoryClassName.indexOf("Factory");
        return index != -1 ? factoryClassName.substring(0, index) : factoryClassName;
    }

    private List<CustomEvent> generateCustomEventSchedule(TestContext context, String text, EventLogger logger, ClassLoader classLoader) {
        EventGenerator eventGenerator;
        EventGeneratorProperties eventGeneratorProperties;

        if (text == null) {
            eventGeneratorProperties = new EventGeneratorProperties();
            EventLoggerWithName myLogger = new EventLoggerWithName("defaultFactory", EventGeneratorDefault.class.getName(), logger);
            eventGenerator = new EventGeneratorFactoryDefault().create(testContext, eventGeneratorProperties, myLogger);
        }
        else if (EventGeneratorProperties.hasLinesThatStartWithMetaPropertyPrefix(text)) {

            eventGeneratorProperties = new EventGeneratorProperties(text);

            String generatorClassname = eventGeneratorProperties.getMetaProperty(EventGeneratorMetaProperty.generatorFactoryClass.name());

            EventGeneratorFactory eventGeneratorFactory = findAndCreateEventScheduleGenerator(logger, generatorClassname, classLoader);

            EventLoggerWithName myLogger = new EventLoggerWithName("customFactory", generatorClassname, logger);
            eventGenerator = eventGeneratorFactory.create(context, eventGeneratorProperties, myLogger);
        }
        else {
            // assume the default input of lines of events
            Map<String, String> properties = new HashMap<>();
            properties.put("eventSchedule", text);

            eventGeneratorProperties = new EventGeneratorProperties(properties);
            EventLoggerWithName myLogger = new EventLoggerWithName("defaultFactory", EventGeneratorDefault.class.getName(), logger);
            eventGenerator = new EventGeneratorFactoryDefault().create(context, eventGeneratorProperties, myLogger);
        }

        return eventGenerator.generate();
    }

    /**
     * Provide schedule event as "duration|eventname(description)|json-settings".
     * The duration is in ISO-8601 format period format, e.g. 3 minutes 15 seconds
     * is PT3M15S.
     *
     * One schedule event per line.
     *
     * Or provide an EventScheduleGenerator implementation as:
     *
     * <pre>
     *      {@literal @}generator-class=nl.stokpop.event.MyEventGenerator
     *      foo=bar
     * </pre>
     *
     * @param customEventsText e.g. PT3M15S|heapdump(1st heapdump)|server=test-server-1
     * @return this
     */
    public EventSchedulerBuilder setCustomEvents(String customEventsText) {
        if (customEventsText != null) {
            this.customEventsText = customEventsText;
        }
        return this;
    }

    private EventGeneratorFactory findAndCreateEventScheduleGenerator(EventLogger logger, String generatorFactoryClassname, ClassLoader classLoader) {
        EventGeneratorFactoryProvider provider =
                EventGeneratorFactoryProvider.createInstanceFromClasspath(logger, classLoader);

        EventGeneratorFactory generatorFactory = provider.find(generatorFactoryClassname);

        if (generatorFactory == null) {
            throw new EventSchedulerRuntimeException("unable to find EventScheduleGeneratorFactory implementation class: " + generatorFactoryClassname);
        }
        return generatorFactory;
    }

    public EventSchedulerBuilder addEvent(String eventName, Map<String, String> properties) {
        addEvent(eventName, new EventProperties(properties));
        return this;
    }

    public EventSchedulerBuilder addEvent(String eventName, Properties properties) {
        addEvent(eventName, new EventProperties(properties));
        return this;
    }

    public EventSchedulerBuilder addEvent(String eventName, EventProperties properties) {
        EventInfo eventInfo = new EventInfo(eventName, properties);
        boolean unique = eventInfos.add(eventInfo);
        if (!unique) {
            throw new EventSchedulerRuntimeException("Event name is not unique: " + eventInfo);
        }
        return this;
    }

    /**
     * Optional. Default is probably good.
     * @param eventFactoryProvider The event factory provider to use.
     */
    EventSchedulerBuilder setEventFactoryProvider(EventFactoryProvider eventFactoryProvider) {
        this.eventFactoryProvider = eventFactoryProvider;
        return this;
    }

    /**
     * Optional. Default is probably good: the async event broadcaster.
     * @param eventBroadcasterFactory the broadcaster implementation to use
     */
    EventSchedulerBuilder setEventBroadcasterFactory(EventBroadcasterFactory eventBroadcasterFactory) {
        this.eventBroadcasterFactory = eventBroadcasterFactory;
        return this;
    }

    /**
     * Event name should be unique: it is used in logging and as lookup key.
     *
     * An event info is considered equal when eventName is the same.
     */
    @Immutable
    private static final class EventInfo {
        private String eventName;
        private EventProperties eventProperties;

        public EventInfo(String eventName, EventProperties eventProperties) {
            this.eventName = eventName;
            this.eventProperties = eventProperties;
        }

        public String getEventName() {
            return eventName;
        }

        public EventProperties getEventProperties() {
            return eventProperties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventInfo eventInfo = (EventInfo) o;
            return Objects.equals(eventName, eventInfo.eventName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventName);
        }

        @Override
        public String toString() {
            return "EventInfo{" +
                    "eventName='" + eventName + '\'' +
                    ", eventProperties=" + eventProperties +
                    '}';
        }
    }
}