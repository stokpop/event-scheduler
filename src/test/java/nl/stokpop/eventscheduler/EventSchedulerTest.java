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

import nl.stokpop.eventscheduler.api.Event;
import nl.stokpop.eventscheduler.api.EventCheck;
import nl.stokpop.eventscheduler.api.EventFactory;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.EventProperties;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.EventSchedulerSettingsBuilder;
import nl.stokpop.eventscheduler.api.EventStatus;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import nl.stokpop.eventscheduler.event.EventFactoryProvider;
import nl.stokpop.eventscheduler.exception.EventCheckFailureException;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * This test class is in same package to use the setEventFactoryProvider call.
 */
public class EventSchedulerTest
{

    @Test
    public void createEventSchedulerAndFireSomeEventsWithFailures() {

        EventLogger testLogger = EventLoggerStdOut.INSTANCE_DEBUG;

        EventFactoryProvider provider = Mockito.mock(EventFactoryProvider.class);
        // to simulate event failures
        Event event = Mockito.mock(Event.class);
        EventFactory eventFactory = Mockito.mock(EventFactory.class);

        Mockito.when(eventFactory.create(any(), any(), any(), any())).thenReturn(event);
        Mockito.when(provider.factoryByClassName(any())).thenReturn(Optional.of(eventFactory));
        EventCheck eventOne = new EventCheck("eventOne", "nl.stokpop.MockEvent", EventStatus.FAILURE, "This event failed!");
        EventCheck eventTwo = new EventCheck("eventTwo", "nl.stokpop.MockEvent", EventStatus.SUCCESS, "This event was ok!");
        EventCheck eventThree = new EventCheck("eventThree", "nl.stokpop.MockEvent", EventStatus.FAILURE, "This event failed also!");
        Mockito.when(event.check()).thenReturn(eventOne).thenReturn(eventTwo).thenReturn(eventThree);

        String eventSchedule =
                "   \n" +
                "    PT1S  |restart   (   restart to reset replicas  )   |{ 'server':'myserver' 'replicas':2, 'tags': [ 'first', 'second' ] }    \n" +
                "PT600S   |scale-down |   { 'replicas':1 }   \n" +
                "PT660S|    heapdump|server=    myserver.example.com;   port=1567  \n" +
                "   PT900S|scale-up|{ 'replicas':2 }\n" +
                "  \n";

        EventSchedulerSettings settings = new EventSchedulerSettingsBuilder()
                .setKeepAliveInterval(Duration.ofMinutes(2))
                .build();

        TestContext context = new TestContextBuilder()
                .setTestType("testType")
                .setTestEnvironment("testEnv")
                .setTestRunId("testRunId")
                .setCIBuildResultsUrl("http://url")
                .setApplicationRelease("release")
                .setRampupTimeInSeconds("10")
                .setConstantLoadTimeInSeconds("300")
                .setAnnotations("annotation")
                .setVariables(Collections.emptyMap())
                .setTags("tag1,tag2")
                .build();

        Properties properties = new Properties();
        properties.put("name", "value");
        properties.put(EventProperties.FACTORY_CLASSNAME_KEY, "nl.stokpop.eventscheduler.event.EventFactoryDefault");

        EventScheduler scheduler = new EventSchedulerBuilder()
                .setEventSchedulerSettings(settings)
                .setTestContext(context)
                .setAssertResultsEnabled(true)
                .addEvent("myEvent1", properties)
                .addEvent("myEvent2", properties)
                .addEvent("myEvent3", properties)
                .setCustomEvents(eventSchedule)
                .setLogger(testLogger)
                .setEventFactoryProvider(provider)
                .build();

        assertNotNull(scheduler);
        assertEquals(120, settings.getKeepAliveDuration().getSeconds());

        scheduler.startSession();
        scheduler.stopSession();

        // this exception is expected to be thrown because two events have reported failures
        String failureMessage = null;
        try {
            scheduler.checkResults();
        } catch (EventCheckFailureException e) {
            failureMessage = e.getMessage();
        }

        assertNotNull("Exception message expected!", failureMessage);
        assertTrue("Should contain the failed event ids.", failureMessage.contains(eventOne.getEventId()) && failureMessage.contains(eventThree.getEventId()));
        assertFalse("Should not contain the success event ids.", failureMessage.contains(eventTwo.getEventId()));

        // note these are called via the lambda catch exception handler via the default broadcaster
        Mockito.verify(event, times(3)).beforeTest();
        Mockito.verify(event, times(3)).afterTest();
        // this seems a timing issue if they are called or not, they are called in ide test, not in gradle test all
        Mockito.verify(event, atMost(3)).keepAlive();

        verifyNoMoreInteractions(ignoreStubs(provider));
        verifyNoMoreInteractions(ignoreStubs(event));
        verifyNoMoreInteractions(ignoreStubs(eventFactory));

    }

    /**
     * Regression: no exceptions expected feeding null
     */
    @Test
    public void createWithNulls() {

        TestContext context = new TestContextBuilder()
                .setAnnotations(null)
                .setApplicationRelease(null)
                .setApplication(null)
                .setCIBuildResultsUrl(null)
                .setConstantLoadTimeInSeconds(null)
                .setConstantLoadTime(null)
                .setRampupTimeInSeconds(null)
                .setRampupTime(null)
                .setTestEnvironment(null)
                .setTestRunId(null)
                .setTestType(null)
                .setVariables((Properties)null)
                .setTags((String)null)
                .build();

        EventSchedulerSettings settings = new EventSchedulerSettingsBuilder()
                .setKeepAliveInterval(null)
                .setKeepAliveTimeInSeconds(null)
                .build();

        new EventSchedulerBuilder()
                .setTestContext(context)
                .setEventSchedulerSettings(settings)
                .setCustomEvents(null)
                .build();
    }

    @Test
    public void createWithFail() {
        EventSchedulerSettings settings =
                new EventSchedulerSettingsBuilder()
                        .setKeepAliveTimeInSeconds("120")
                        .build();

        assertNotNull(settings);
    }

}
