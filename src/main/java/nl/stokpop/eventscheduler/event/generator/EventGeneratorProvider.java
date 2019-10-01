package nl.stokpop.eventscheduler.event.generator;

import nl.stokpop.eventscheduler.api.PerfanaClientLogger;
import nl.stokpop.eventscheduler.event.EventGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class EventGeneratorProvider {

    private Map<String, EventGenerator> generators;
    private PerfanaClientLogger logger;

    EventGeneratorProvider(Map<String, EventGenerator> generators, PerfanaClientLogger logger) {
        this.generators = Collections.unmodifiableMap(new HashMap<>(generators));
        this.logger = logger;
    }

    public static EventGeneratorProvider createInstanceFromClasspath(PerfanaClientLogger logger) {
        return createInstanceFromClasspath(logger, null);
    }

    public static EventGeneratorProvider createInstanceFromClasspath(PerfanaClientLogger logger, ClassLoader classLoader) {
        ServiceLoader<EventGenerator> generatorLoader = classLoader == null
                ? ServiceLoader.load(EventGenerator.class)
                : ServiceLoader.load(EventGenerator.class, classLoader);
        // java 9+: List<PerfanaEvent> generators = perfanaEventLoader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
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
