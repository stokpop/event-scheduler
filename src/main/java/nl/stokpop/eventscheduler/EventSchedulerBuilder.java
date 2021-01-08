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
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.EventSchedulerSettingsBuilder;
import nl.stokpop.eventscheduler.api.config.EventConfig;
import nl.stokpop.eventscheduler.api.config.EventSchedulerConfig;
import nl.stokpop.eventscheduler.api.config.TestConfig;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builder: intended to be used in one thread for construction and then to be discarded.
 */
@NotThreadSafe
public class EventSchedulerBuilder {

    public static EventScheduler of(EventSchedulerConfig eventSchedulerConfig, EventLogger logger) {
        return of(eventSchedulerConfig, logger, null);
    }

    /**
     * Create an EventScheduler from an EventSchedulerConfig.
     * @param eventSchedulerConfig note that this eventSchedulerConfig will be modified in this method (beh: better make immutable)
     * @param logger the EventLogger for log lines from the EventScheduler and its construction
     * @param classLoader needed in cased where the dynamic class creation does not work in default classloader, can be null
     * @return a fully constructed EventScheduler
     */
    public static EventScheduler of(EventSchedulerConfig eventSchedulerConfig, EventLogger logger, ClassLoader classLoader) {

        final TestConfig topLevelConfig = determineTopLevelConfigAndInjectInEventConfigs(eventSchedulerConfig, logger);

        String totalScheduleScript = collectScheduleScriptsInTopLevelConfig(eventSchedulerConfig);
        eventSchedulerConfig.setScheduleScript(totalScheduleScript);

        EventSchedulerSettings settings = new EventSchedulerSettingsBuilder()
            .setKeepAliveInterval(Duration.ofSeconds(eventSchedulerConfig.getKeepAliveIntervalInSeconds()))
            .build();

        EventSchedulerBuilderInternal eventSchedulerBuilder = new EventSchedulerBuilderInternal()
            .setName(topLevelConfig.getTestRunId())
            .setEventSchedulerSettings(settings)
            .setAssertResultsEnabled(eventSchedulerConfig.isSchedulerEnabled())
            .setCustomEvents(eventSchedulerConfig.getScheduleScript())
            .setLogger(logger);

        if (eventSchedulerConfig.getEventConfigs() != null) {
            eventSchedulerConfig.getEventConfigs().forEach(eventSchedulerBuilder::addEvent);
        }

        return eventSchedulerBuilder.build(classLoader);
    }

    private static String collectScheduleScriptsInTopLevelConfig(EventSchedulerConfig eventSchedulerConfig) {
        String topLevelScheduleScript = eventSchedulerConfig.getScheduleScript();

        String subScheduleScripts = eventSchedulerConfig.getEventConfigs().stream()
            .map(EventConfig::getScheduleScript)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));

        if (topLevelScheduleScript == null) {
            return removeEmptyLines(subScheduleScripts);
        }
        else {
            return removeEmptyLines(String.join("\n", topLevelScheduleScript, subScheduleScripts));
        }

    }

    private static String removeEmptyLines(String text) {
        return Arrays.stream(text.split("\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining("\n"));
    }

    private static TestConfig determineTopLevelConfigAndInjectInEventConfigs(EventSchedulerConfig eventSchedulerConfig, EventLogger logger) {

        final TestConfig topLevelConfig;
        // find first event config with a test config to use as top level config
        if (eventSchedulerConfig.getTestConfig() == null) {
            logger.info("no top level TestConfig found, will look for TestConfig in EventConfigs");

            List<EventConfig> eventConfigsWithTestConfig = eventSchedulerConfig.getEventConfigs().stream()
                .filter(eventConfig -> eventConfig.getTestConfig() != null)
                .collect(Collectors.toList());

            if (eventConfigsWithTestConfig.isEmpty()) {
                throw new EventSchedulerRuntimeException("no EventConfig found with a TestConfig, add one TestConfig");
            }

            if (eventConfigsWithTestConfig.size() > 1) {
                throw new EventSchedulerRuntimeException("multiple EventConfigs found with a TestConfig, only one TestConfig is allowed without using a top level TestConfig");
            }

            EventConfig eventConfig = eventConfigsWithTestConfig.get(0);
            logger.info("using TestConfig of EventConfig '" + eventConfig.getName() + "' as top level TestConfig");

            topLevelConfig = eventConfig.getTestConfig();
            eventSchedulerConfig.setTestConfig(topLevelConfig);
        }
        else {
            topLevelConfig = eventSchedulerConfig.getTestConfig();
            // if there is a top level test config, do not allow test config in event configs
            List<EventConfig> eventConfigsWithTestConfig = eventSchedulerConfig.getEventConfigs().stream()
                .filter(eventConfig -> eventConfig.getTestConfig() != null)
                .collect(Collectors.toList());

            if (eventConfigsWithTestConfig.size() > 0) {
                String eventConfigs = eventConfigsWithTestConfig.stream()
                    .map(EventConfig::getName)
                    .collect(Collectors.joining(","));
                throw new EventSchedulerRuntimeException("when a top level TestConfig is used, do not use TestConfig in EventConfigs: " + eventConfigs);
            }
        }

        // inject top level config in all event configs
        eventSchedulerConfig.getEventConfigs()
            .forEach(eventConfig -> eventConfig.setTestConfig(topLevelConfig));

        return topLevelConfig;
    }
}