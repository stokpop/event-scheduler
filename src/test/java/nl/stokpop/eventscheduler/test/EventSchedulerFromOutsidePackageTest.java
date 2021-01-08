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
package nl.stokpop.eventscheduler.test;

import nl.stokpop.eventscheduler.EventScheduler;
import nl.stokpop.eventscheduler.EventSchedulerBuilder;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.EventSchedulerSettingsBuilder;
import nl.stokpop.eventscheduler.api.config.EventConfig;
import nl.stokpop.eventscheduler.api.config.EventSchedulerConfig;
import nl.stokpop.eventscheduler.api.config.TestConfig;
import nl.stokpop.eventscheduler.log.CountErrorsEventLogger;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test class is in another package to check access package private fields.
 */
public class EventSchedulerFromOutsidePackageTest
{
    @Test
    public void createEventSchedulerAndFireSomeEvents() {

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        String scheduleScript1 =
                "   \n" +
                "PT600S   |scale-down |   { 'replicas':1 }   \n" +
                "PT660S|    heapdump|server=    myserver.example.com;   port=1567  \n" +
                "  \n";

        String scheduleScript2 =
                "   \n" +
                "    PT1S  |restart   (   restart to reset replicas  )   |{ 'server':'myserver' 'replicas':2, 'tags': [ 'first', 'second' ] }    \n" +
                "PT660S|    heapdump|server=    myserver.example.com;   port=1567  \n" +
                "  \n";

        EventSchedulerSettings settings = new EventSchedulerSettingsBuilder()
                .setKeepAliveInterval(Duration.ofMinutes(2))
                .build();

        TestConfig testConfig = TestConfig.builder()
            .workload("testType")
            .testEnvironment("testEnv")
            .testRunId("testRunId")
            .buildResultsUrl("http://url")
            .version("version")
            .rampupTimeInSeconds(10)
            .constantLoadTimeInSeconds(300)
            .annotations("annotation")
            .variables(Collections.emptyMap())
            .tags(Arrays.asList("tag1","tag2"))
            .build();

        // this class really needs to be on the classpath, otherwise: runtime exception, not found on classpath
        String factoryClassName = "nl.stokpop.eventscheduler.event.EventFactoryDefault";

        List<EventConfig> eventConfigs = new ArrayList<>();
        eventConfigs.add(EventConfig.builder().name("myEvent1").eventFactory(factoryClassName).scheduleScript(scheduleScript2).build());
        eventConfigs.add(EventConfig.builder().name("myEvent2").eventFactory(factoryClassName).build());
        eventConfigs.add(EventConfig.builder().name("myEvent3").eventFactory(factoryClassName).build());

        EventSchedulerConfig eventSchedulerConfig = EventSchedulerConfig.builder()
            .schedulerEnabled(true)
            .debugEnabled(false)
            .continueOnAssertionFailure(false)
            .failOnError(true)
            .testConfig(testConfig)
            .eventConfigs(eventConfigs)
            .scheduleScript(scheduleScript1)
            .build();

        EventScheduler scheduler = EventSchedulerBuilder.of(eventSchedulerConfig, countErrorsEventLogger);

        assertEquals("4 lines expected in total scheduler script", 4, eventSchedulerConfig.getScheduleScript().split("\\n").length);

        assertNotNull(scheduler);
        assertEquals(120, settings.getKeepAliveDuration().getSeconds());
        scheduler.startSession();
        scheduler.stopSession();

        // no failure exception expected
        assertEquals("zero errors expected in logger", 0, countErrorsEventLogger.errorCount());
        scheduler.checkResults();
    }

}
