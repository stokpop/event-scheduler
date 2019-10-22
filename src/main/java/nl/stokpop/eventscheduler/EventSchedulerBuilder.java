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
package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.EventGeneratorFactory;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.event.CustomEvent;
import nl.stokpop.eventscheduler.event.EventBroadcaster;
import nl.stokpop.eventscheduler.event.EventGenerator;
import nl.stokpop.eventscheduler.event.EventSchedulerProperties;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryProvider;
import nl.stokpop.eventscheduler.generator.EventGeneratorProperties;
import nl.stokpop.eventscheduler.log.EventLoggerDevNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventSchedulerBuilder {

    private static final String GENERATOR_CLASS_META_TAG = "@generator-class";

    private TestContext testContext;

    private EventSchedulerSettings eventSchedulerSettings;

    private boolean assertResultsEnabled = false;

    private EventBroadcaster broadcaster;
    private EventSchedulerProperties eventProperties = new EventSchedulerProperties();

    private String customEventsText = "";

    private EventLogger logger = EventLoggerDevNull.INSTANCE;

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

    public EventSchedulerBuilder setBroadcaster(EventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
        return this;
    }
    
    /**
     * Add properties to be passed on to the event implementation class.
     * @param eventImplementationName the fully qualified implementation class name (class.getName())
     * @param name the name of the property (not null or empty), e.g. "REST_URL"
     * @param value the name of the property (can be null or empty), e.g. "https://my-rest-call"
     * @return this
     */
    public EventSchedulerBuilder addEventProperty(String eventImplementationName, String name, String value) {
        if (eventImplementationName == null || eventImplementationName.isEmpty()) {
            throw new EventSchedulerRuntimeException("EventImplementationName is null or empty for " + this);
        }
        if (name == null || name.isEmpty()) {
            throw new EventSchedulerRuntimeException("EventImplementation property name is null or empty for " + this);
        }
        eventProperties.put(eventImplementationName, name, value);
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

        // get default broadcaster if no broadcaster was given
        if (broadcaster == null) {
            throw new EventSchedulerRuntimeException("Broadcaster must be set, it is null.");
        }
        
        if (testContext == null) {
            throw new EventSchedulerRuntimeException("TestContext must be set, it is null.");
        }

        if (eventSchedulerSettings == null) {
            throw new EventSchedulerRuntimeException("EventSchedulerSettings must be set, it is null.");
        }

        List<CustomEvent> customEvents = generateCustomEventSchedule(testContext, customEventsText, logger, classLoader);

        return new EventScheduler(testContext, eventSchedulerSettings, assertResultsEnabled,
                broadcaster, eventProperties, customEvents, logger);
    }

    private List<CustomEvent> generateCustomEventSchedule(TestContext context, String text, EventLogger logger, ClassLoader classLoader) {
        EventGenerator eventGenerator;
        EventGeneratorProperties eventGeneratorProperties;

        if (text == null) {
            eventGeneratorProperties = new EventGeneratorProperties();
            eventGenerator = new EventGeneratorFactoryDefault().create(testContext, eventGeneratorProperties);
        }
        else if (text.contains(GENERATOR_CLASS_META_TAG)) {

            eventGeneratorProperties = new EventGeneratorProperties(text);

            String generatorClassname = eventGeneratorProperties.getMetaProperty(GENERATOR_CLASS_META_TAG);

            EventGeneratorFactory eventGeneratorFactory = findAndCreateEventScheduleGenerator(logger, generatorClassname, classLoader);
            eventGenerator = eventGeneratorFactory.create(context, eventGeneratorProperties);
        }
        else {
            // assume the default input of lines of events
            Map<String, String> properties = new HashMap<>();
            properties.put("eventSchedule", text);

            eventGeneratorProperties = new EventGeneratorProperties(properties);
            eventGenerator = new EventGeneratorFactoryDefault().create(context, eventGeneratorProperties);
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
    
}