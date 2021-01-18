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
package nl.stokpop.eventscheduler.api.config;

import lombok.*;
import net.jcip.annotations.NotThreadSafe;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NotThreadSafe
public class EventSchedulerConfig {
    @Builder.Default
    private boolean debugEnabled = false;
    @Builder.Default
    private boolean schedulerEnabled = true;
    @Builder.Default
    private boolean failOnError = true;
    @Builder.Default
    private boolean continueOnEventCheckFailure = true;
    @Builder.Default
    private Integer keepAliveIntervalInSeconds = 30;
    @Builder.Default
    private String scheduleScript = null;
    @Singular
    private List<EventConfig> eventConfigs;
    @Builder.Default
    private TestConfig testConfig = null;

    public EventSchedulerContext toContext(EventLogger logger) {

        List<EventContext> eventConfigsWithTestConfig = eventConfigs.stream()
            .map(EventConfig::toContext)
            .filter(c -> c.getTestContext() != null)
            .collect(Collectors.toList());

        final TestContext topLevelContext = determineTestContext(logger, eventConfigsWithTestConfig);

        // inject top level config in all event contexts
        List<EventContext> eventContextsWithTopLevelConfig = eventConfigs.stream()
            .map(c -> c.toContext(topLevelContext))
            .collect(Collectors.toList());

        String allScheduleScripts = collectScheduleScripts(eventContextsWithTopLevelConfig, this.scheduleScript);

        return EventSchedulerContext.builder()
            .debugEnabled(debugEnabled)
            .schedulerEnabled(schedulerEnabled)
            .failOnError(failOnError)
            .continueOnEventCheckFailure(continueOnEventCheckFailure)
            .keepAliveInterval(Duration.ofSeconds(keepAliveIntervalInSeconds))
            .scheduleScript(allScheduleScripts)
            .eventContexts(eventContextsWithTopLevelConfig)
            .testContext(topLevelContext)
            .build();
    }

    private TestContext determineTestContext(EventLogger logger, List<EventContext> eventConfigsWithTestConfig) {
        final TestContext topLevelContext;
        // find first event config with a test config to use as top level config
        if (testConfig == null) {
            logger.info("no top level TestConfig found, will look for TestConfig in EventConfigs");

            if (eventConfigsWithTestConfig.isEmpty()) {
                throw new EventSchedulerRuntimeException("no EventConfig found with a TestConfig, add one TestConfig");
            }

            if (eventConfigsWithTestConfig.size() > 1) {
                throw new EventSchedulerRuntimeException("multiple EventConfigs found with a TestConfig, only one TestConfig is allowed without using a top level TestConfig");
            }

            EventContext eventContext = eventConfigsWithTestConfig.get(0);
            logger.info("using TestConfig of EventConfig '" + eventContext.getName() + "' as top level TestConfig");

            topLevelContext = eventContext.getTestContext();
        }
        else {
            topLevelContext = testConfig.toContext();
            // if there is a top level test config, do not allow test config in event configs
            if (eventConfigsWithTestConfig.size() > 0) {
                String eventConfigs = eventConfigsWithTestConfig.stream()
                    .map(EventContext::getName)
                    .collect(Collectors.joining(","));
                throw new EventSchedulerRuntimeException("when a top level TestConfig is used, do not use TestConfig in EventConfigs: " + eventConfigs);
            }
        }
        return topLevelContext;
    }

    private static String collectScheduleScripts(List<EventContext> eventContexts, String topScheduleScript) {

        String allSubScripts = eventContexts.stream()
            .map(EventContext::getScheduleScript)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));

        if (topScheduleScript == null) {
            return removeEmptyLines(allSubScripts);
        }
        else {
            return removeEmptyLines(String.join("\n", topScheduleScript, allSubScripts));
        }

    }

    private static String removeEmptyLines(String text) {
        return Arrays.stream(text.split("\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining("\n"));
    }

}
