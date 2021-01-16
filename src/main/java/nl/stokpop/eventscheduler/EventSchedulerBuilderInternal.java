/*
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
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

import net.jcip.annotations.NotThreadSafe;
import nl.stokpop.eventscheduler.api.*;
import nl.stokpop.eventscheduler.api.config.EventConfig;
import nl.stokpop.eventscheduler.api.message.EventMessageBus;
import nl.stokpop.eventscheduler.event.EventFactoryProvider;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.generator.EventGeneratorDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryProvider;
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;
import nl.stokpop.eventscheduler.log.EventLoggerWithName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Builder: intended to be used in one thread for construction and then to be discarded.
 */
@NotThreadSafe
class EventSchedulerBuilderInternal {

    private final Map<String, EventConfig> eventConfigs = new ConcurrentHashMap<>();

    private String name;

    private EventSchedulerSettings eventSchedulerSettings;

    private boolean assertResultsEnabled = false;

    private String customEventsText = "";

    private EventLogger logger = EventLoggerDevNull.INSTANCE;

    private EventFactoryProvider eventFactoryProvider;

    private EventBroadcasterFactory eventBroadcasterFactory;

    private SchedulerExceptionHandler schedulerExceptionHandler;

    private EventSchedulerEngine eventSchedulerEngine;

    private EventMessageBus eventMessageBus;

    public EventSchedulerBuilderInternal setEventSchedulerEngine(EventSchedulerEngine executorEngine) {
        this.eventSchedulerEngine = executorEngine;
        return this;
    }

    public EventSchedulerBuilderInternal setEventMessageBus(EventMessageBus eventMessageBus) {
        this.eventMessageBus = eventMessageBus;
        return this;
    }

    public EventSchedulerBuilderInternal setSchedulerExceptionHandler(SchedulerExceptionHandler callback) {
        this.schedulerExceptionHandler = callback;
        return this;
    }
    public EventSchedulerBuilderInternal setName(String name) {
        this.name = name;
        return this;
    }

    public EventSchedulerBuilderInternal setLogger(EventLogger logger) {
        this.logger = logger;
        return this;
    }

    public EventSchedulerBuilderInternal setEventSchedulerSettings(EventSchedulerSettings settings) {
        this.eventSchedulerSettings = settings;
        return this;
    }

    public EventSchedulerBuilderInternal setAssertResultsEnabled(boolean assertResultsEnabled) {
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

        if (name == null) {
            throw new EventSchedulerRuntimeException("Name must be set, it is null.");
        }

        EventSchedulerSettings myEventSchedulerSettings = (eventSchedulerSettings == null)
                ? new EventSchedulerSettingsBuilder().build()
                : eventSchedulerSettings;

        List<CustomEvent> customEvents =
                generateCustomEventSchedule(customEventsText, logger, classLoader);

        EventFactoryProvider provider = (eventFactoryProvider == null)
                ? EventFactoryProvider.createInstanceFromClasspath(classLoader)
                : eventFactoryProvider;

        eventConfigs.values().stream()
                .filter(eventConfig -> !eventConfig.isEnabled())
                .forEach(eventConfig -> logger.info("Event disabled: " + eventConfig.getName()));

        List<Event> events = eventConfigs.values().stream()
                .filter(EventConfig::isEnabled)
                .map(eventConfig -> createEvent(provider, eventConfig, eventMessageBus))
                .collect(Collectors.toList());

        EventBroadcasterFactory broadcasterFactory = (eventBroadcasterFactory == null)
                ? EventBroadcasterAsync::new
                : eventBroadcasterFactory;

        EventBroadcaster broadcaster = broadcasterFactory.create(events, logger);

        eventSchedulerEngine = (eventSchedulerEngine == null)
            ? new EventSchedulerEngine(logger)
            : eventSchedulerEngine;

        EventMessageBus eventMessageBus = (this.eventMessageBus == null)
            ? new EventMessageBusImpl()
            : this.eventMessageBus;

        return new EventScheduler(
            name,
            myEventSchedulerSettings,
            assertResultsEnabled,
            broadcaster,
            customEvents,
            eventConfigs.values(),
            eventMessageBus,
            logger,
            eventSchedulerEngine,
            schedulerExceptionHandler);
    }

    @SuppressWarnings("unchecked")
    private Event createEvent(EventFactoryProvider provider, EventConfig eventConfig, EventMessageBus eventMessageBus) {
        String factoryClassName = eventConfig.getEventFactory();
        String eventName = eventConfig.getName();
        EventLogger eventLogger = new EventLoggerWithName(eventName, removeFactoryPostfix(factoryClassName), logger);

        // create has raw type usage, so we have @SuppressWarnings("unchecked")
        return provider.factoryByClassName(factoryClassName)
                .orElseThrow(() -> new RuntimeException(factoryClassName + " not found on classpath"))
                .create(eventConfig, eventMessageBus, eventLogger);
    }

    private String removeFactoryPostfix(String factoryClassName) {
        int index = factoryClassName.indexOf("Factory");
        return index != -1 ? factoryClassName.substring(0, index) : factoryClassName;
    }

    private List<CustomEvent> generateCustomEventSchedule(String text, EventLogger logger, ClassLoader classLoader) {
        EventGenerator eventGenerator;
        EventGeneratorProperties eventGeneratorProperties;

        if (text == null) {
            eventGeneratorProperties = new EventGeneratorProperties();
            EventLoggerWithName myLogger = new EventLoggerWithName("defaultFactory", EventGeneratorDefault.class.getName(), logger);
            eventGenerator = new EventGeneratorFactoryDefault().create(eventGeneratorProperties, myLogger);
        }
        else if (EventGeneratorProperties.hasLinesThatStartWithMetaPropertyPrefix(text)) {

            eventGeneratorProperties = new EventGeneratorProperties(text);

            String generatorClassname = eventGeneratorProperties.getMetaProperty(EventGeneratorMetaProperty.generatorFactoryClass.name());

            EventGeneratorFactory eventGeneratorFactory = findAndCreateEventScheduleGenerator(logger, generatorClassname, classLoader);

            EventLoggerWithName myLogger = new EventLoggerWithName("customFactory", generatorClassname, logger);
            eventGenerator = eventGeneratorFactory.create(eventGeneratorProperties, myLogger);
        }
        else {
            // assume the default input of lines of events
            Map<String, String> properties = new HashMap<>();
            properties.put("eventSchedule", text);

            eventGeneratorProperties = new EventGeneratorProperties(properties);
            EventLoggerWithName myLogger = new EventLoggerWithName("defaultFactory", EventGeneratorDefault.class.getName(), logger);
            eventGenerator = new EventGeneratorFactoryDefault().create(eventGeneratorProperties, myLogger);
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
     * @param customEventsText e.g. PT3M15S|heapdump(1st heapdump)|{'server': 'test-server-1'}
     * @return this
     */
    public EventSchedulerBuilderInternal setCustomEvents(String customEventsText) {
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

    /**
     * Event name should be unique: it is used in logging and as lookup key.
     * @param eventConfig the config to add, the eventConfig must contain an testConfig
     * @return this
     */
    public EventSchedulerBuilderInternal addEvent(EventConfig eventConfig) {
        if (eventConfig.getTestConfig() == null) { throw new EventSchedulerRuntimeException("eventConfig without test config! " + eventConfig); }
        EventConfig existingEventConfig = eventConfigs.putIfAbsent(eventConfig.getName(), eventConfig);
        if (existingEventConfig != null) {
            throw new EventSchedulerRuntimeException("Event name is not unique: " + eventConfig.getName());
        }
        return this;
    }

    /**
     * Optional. Default is probably good.
     * @param eventFactoryProvider The event factory provider to use.
     */
    EventSchedulerBuilderInternal setEventFactoryProvider(EventFactoryProvider eventFactoryProvider) {
        this.eventFactoryProvider = eventFactoryProvider;
        return this;
    }

    /**
     * Optional. Default is probably good: the async event broadcaster.
     * @param eventBroadcasterFactory the broadcaster implementation to use
     */
    EventSchedulerBuilderInternal setEventBroadcasterFactory(EventBroadcasterFactory eventBroadcasterFactory) {
        this.eventBroadcasterFactory = eventBroadcasterFactory;
        return this;
    }

}