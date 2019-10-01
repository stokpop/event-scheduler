package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.EventSchedulerLogger;
import nl.stokpop.eventscheduler.api.EventSchedulerLoggerStdOut;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.event.*;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.generator.EventGeneratorDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorProvider;
import nl.stokpop.eventscheduler.generator.EventGeneratorProperties;

import java.util.Collections;
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

    private EventSchedulerLogger logger = new EventSchedulerLoggerStdOut();

    public EventSchedulerBuilder setTestContext(TestContext context) {
        this.testContext = context;
        return this;
    }

    public EventSchedulerBuilder setLogger(EventSchedulerLogger logger) {
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
     * By default the classloader from the current thread is used to load event providers and related resources
     * (see .
     *
     * For example in a Gradle plugin the thread classpath is limited to plugin classes,
     * and does not contain classes from the project context, such as the custom event providers used in the project.
     *
     * @param classLoader the class loader, if null the default classloader of Java's ServiceLoader will be used
     * @return a new PerfanaClient
     */
    public EventScheduler build(ClassLoader classLoader) {

        // get default broadcaster if no broadcaster was given
        if (broadcaster == null) {
            logger.info("create default Perfana event broadcaster");
            broadcaster = EventProvider.createInstanceWithEventsFromClasspath(logger, classLoader);
        }
        
        if (testContext == null) {
            throw new EventSchedulerRuntimeException("TestContext must be set, it is null.");
        }

        if (eventSchedulerSettings == null) {
            throw new EventSchedulerRuntimeException("PerfanaConnectionSettings must be set, it is null.");
        }

        List<ScheduleEvent> scheduleEvents = generateEventSchedule(testContext, customEventsText, logger, classLoader);

        return new EventScheduler(testContext, eventSchedulerSettings, assertResultsEnabled,
                broadcaster, eventProperties, scheduleEvents, logger);
    }

    private List<ScheduleEvent> generateEventSchedule(TestContext context, String text, EventSchedulerLogger logger, ClassLoader classLoader) {
        EventGenerator eventGenerator;
        EventGeneratorProperties eventGeneratorProperties;

        if (text == null) {
            eventGenerator = new EventGeneratorDefault();
            eventGeneratorProperties = new EventGeneratorProperties();
        }
        else if (text.contains(GENERATOR_CLASS_META_TAG)) {

            eventGeneratorProperties = new EventGeneratorProperties(text);

            String generatorClassname = eventGeneratorProperties.getMetaProperty(GENERATOR_CLASS_META_TAG);

            eventGenerator = findAndCreateEventScheduleGenerator(logger, generatorClassname, classLoader);
        }
        else {
            // assume the default input of lines of events
            Map<String, String> properties = new HashMap<>();
            properties.put("eventSchedule", text);

            eventGenerator = new EventGeneratorDefault();
            eventGeneratorProperties = new EventGeneratorProperties(properties);
        }

        List<ScheduleEvent> scheduleEvents = Collections.emptyList();
        scheduleEvents = eventGenerator.generate(context, eventGeneratorProperties);
        return scheduleEvents;
    }

    /**
     * Provide schedule event as "duration|eventname(description)|json-settings".
     * The duration is in ISO-8601 format period format, e.g. 3 minutes 15 seconds
     * is P3M15S.
     *
     * One schedule event per line.
     *
     * Or provide an EventScheduleGenerator implementation as:
     *
     * <pre>
     *      {@literal @}generator-class=nl.stokpop.event.MyEventGenerator
     *      foo=bar
     * </pre>
     */
    public EventSchedulerBuilder setCustomEvents(String customEventsText) {
        if (customEventsText != null) {
            this.customEventsText = customEventsText;
        }
        return this;
    }

    private EventGenerator findAndCreateEventScheduleGenerator(EventSchedulerLogger logger, String generatorClassname, ClassLoader classLoader) {
        EventGeneratorProvider provider =
                EventGeneratorProvider.createInstanceFromClasspath(logger, classLoader);

        EventGenerator generator = provider.find(generatorClassname);

        if (generator == null) {
            throw new EventSchedulerRuntimeException("unable to find EventScheduleGenerator implementation class: " + generatorClassname);
        }
        return generator;
    }
    
}