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

import nl.stokpop.eventscheduler.api.*;
import nl.stokpop.eventscheduler.api.config.*;
import nl.stokpop.eventscheduler.api.message.EventMessage;
import nl.stokpop.eventscheduler.api.message.EventMessageBus;
import nl.stokpop.eventscheduler.event.EventFactoryProvider;
import nl.stokpop.eventscheduler.exception.EventCheckFailureException;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        @SuppressWarnings("unchecked")
        EventFactory<EventContext> eventFactory = Mockito.mock(EventFactory.class);

        Mockito.when(eventFactory.create(any(), any(), any())).thenReturn(event);
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

        TestConfig testConfig = TestConfig.builder()
            .workload("testType")
            .testEnvironment("testEnv")
            .testRunId("testRunId")
            .buildResultsUrl("http://url")
            .version("version")
            .rampupTimeInSeconds(10)
            .constantLoadTimeInSeconds(300)
            .annotations("annotation")
            .tags(Arrays.asList("tag1","tag2"))
            .build();

        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .eventConfig(EventConfig.builder().name("myEvent1").testConfig(testConfig).build())
            .eventConfig(EventConfig.builder().name("myEvent2").build())
            .eventConfig(EventConfig.builder().name("myEvent3").build())
            .build();

        EventSchedulerContext schedulerContext = config.toContext(EventLoggerStdOut.INSTANCE);

        EventScheduler scheduler = new EventSchedulerBuilderInternal()
                .setEventSchedulerContext(schedulerContext)
                .setCustomEvents(eventSchedule)
                .setLogger(testLogger)
                .setEventFactoryProvider(provider)
                .build();

        assertEquals(30, schedulerContext.getKeepAliveInterval().getSeconds());
        assertNotNull(scheduler);

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
        Mockito.verify(event, times(3)).startTest();
        Mockito.verify(event, times(3)).afterTest();
        // this seems a timing issue if they are called or not, they are called in ide test, not in gradle test all
        Mockito.verify(event, atMost(3)).keepAlive();

        verifyNoMoreInteractions(ignoreStubs(provider));
        verifyNoMoreInteractions(ignoreStubs(event));
        verifyNoMoreInteractions(ignoreStubs(eventFactory));

    }

    static class EventWithMessageBus extends EventAdapter<EventContext> {

        public AtomicInteger startTestCounter = new AtomicInteger(0);

        public EventWithMessageBus(EventContext eventContext, EventLogger logger, EventMessageBus eventMessageBus) {
            super(eventContext, eventMessageBus, logger);
            eventMessageBus.addReceiver(message -> System.out.println(eventContext.getName() + " received " + message));
        }

        @Override
        public void beforeTest() {
            super.beforeTest();
            EventMessage message = EventMessage.builder()
                .pluginName(EventWithMessageBus.class.getSimpleName() + "-" + eventContext.getName())
                .message("Go!")
                .build();
            eventMessageBus.send(message);
        }

        @Override
        public void startTest() {
            super.startTest();
            startTestCounter.incrementAndGet();
        }
    }

    @Test
    public void createEventSchedulerWithMessageBus() {

        EventLogger testLogger = EventLoggerStdOut.INSTANCE_DEBUG;

        EventFactoryProvider provider = Mockito.mock(EventFactoryProvider.class);

        @SuppressWarnings("unchecked")
        EventFactory<EventContext> eventFactory = Mockito.mock(EventFactory.class);

        TestConfig testConfig = TestConfig.builder().build();

        EventConfig eventConfig1 = EventConfig.builder()
            .name("myEvent1").build();
        EventConfig eventConfig2 = EventConfig.builder()
            .name("myEvent2").testConfig(testConfig).build();

        EventMessageBusSimple eventMessageBus = new EventMessageBusSimple();
        EventWithMessageBus event1 = new EventWithMessageBus(eventConfig1.toContext(), testLogger, eventMessageBus);
        EventWithMessageBus event2 = new EventWithMessageBus(eventConfig2.toContext(), testLogger, eventMessageBus);

        Mockito.when(eventFactory.create(any(), any(), any())).thenReturn(event1).thenReturn(event2);
        Mockito.when(provider.factoryByClassName(any())).thenReturn(Optional.of(eventFactory));

        EventSchedulerConfig eventSchedulerConfig = EventSchedulerConfig.builder()
            .keepAliveIntervalInSeconds(1)
            .eventConfig(eventConfig1)
            .eventConfig(eventConfig2)
            .build();

        EventScheduler scheduler = new EventSchedulerBuilderInternal()
                .setEventSchedulerContext(eventSchedulerConfig.toContext(testLogger))
                .setLogger(testLogger)
                .setEventFactoryProvider(provider)
                .setEventMessageBus(eventMessageBus)
                .build();

        scheduler.startSession();

        assertEquals(1, event1.startTestCounter.get());
        assertEquals(1, event2.startTestCounter.get());
    }

    @Test
    public void createEventSchedulerWithMessageBusAndReadyToStartParticipants() {

        EventLogger testLogger = EventLoggerStdOut.INSTANCE_DEBUG;

        EventFactoryProvider provider = Mockito.mock(EventFactoryProvider.class);

        @SuppressWarnings("unchecked")
        EventFactory<EventContext> eventFactory = Mockito.mock(EventFactory.class);

        TestConfig testConfig = TestConfig.builder().build();

        EventConfig eventConfig1 = EventConfig.builder()
            .name("myEvent1")
            .isReadyForStartParticipant(true)
            .testConfig(testConfig)
            .build();
        EventConfig eventConfig2 = EventConfig.builder()
            .name("myEvent2")
            .isReadyForStartParticipant(true)
            .build();

        EventMessageBusSimple eventMessageBus = new EventMessageBusSimple();
        EventWithMessageBus event1 = new EventWithMessageBus(eventConfig1.toContext(), testLogger, eventMessageBus);
        EventWithMessageBus event2 = new EventWithMessageBus(eventConfig2.toContext(), testLogger, eventMessageBus);

        Mockito.when(eventFactory.create(any(), any(), any())).thenReturn(event1).thenReturn(event2);
        Mockito.when(provider.factoryByClassName(any())).thenReturn(Optional.of(eventFactory));

        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .keepAliveIntervalInSeconds(1)
            .eventConfig(eventConfig1)
            .eventConfig(eventConfig2)
            .build();

        EventScheduler scheduler = new EventSchedulerBuilderInternal()
                .setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE))
                .setEventMessageBus(eventMessageBus)
                .setLogger(testLogger)
                .setEventFactoryProvider(provider)
                .build();

        scheduler.startSession();

        assertEquals(1, event1.startTestCounter.get());
        assertEquals(1, event2.startTestCounter.get());
    }

    /**
     * Regression: no exceptions expected feeding null
     */
    @Test
    public void createWithNulls() {

        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .testConfig(TestConfig.builder().build())
            .keepAliveIntervalInSeconds(1)
            .build();

        new EventSchedulerBuilderInternal()
                .setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE))
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

    @Test
    public void createWithDisabledEvent() {
        TestConfig testConfig = TestConfig.builder().build();

        AtomicInteger countInfoMessages = new AtomicInteger();

        EventLogger eventLogger = new EventLoggerStdOut(true) {
            @Override
            public void info(String message) {
                super.info(message);
                if (message.contains("Before test:")) {
                    countInfoMessages.incrementAndGet();
                }
            }
        };

        String eventFactory = "nl.stokpop.eventscheduler.event.EventFactoryDefault";
        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .eventConfig(EventConfig.builder().name("eventEnabled1").eventFactory(eventFactory).testConfig(testConfig).enabled(true).build())
            .eventConfig(EventConfig.builder().name("eventEnabled2").eventFactory(eventFactory).enabled(true).build())
            .eventConfig(EventConfig.builder().name("eventDisabled").eventFactory(eventFactory).enabled(false).build())
            .build();

        EventScheduler scheduler = new EventSchedulerBuilderInternal()
            // avoid timing issues: do not use the default async broadcaster
            .setEventBroadcasterFactory(EventBroadcasterDefault::new)
            .setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE))
            .setLogger(eventLogger)
            .build();

        // expect "before test" event called for 2 enabled instances
        scheduler.startSession();

        assertEquals(2, countInfoMessages.get());

    }

    @Test
    public void createWithUnknownProperty()  {
        EventConfig eventConfig = EventConfig.builder()
            .name("myEvent")
            .enabled(true)
            .eventFactory("nl.stokpop.eventscheduler.event.EventFactoryDefault")
            .testConfig(TestConfig.builder().build())
            .build();

        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .eventConfig(eventConfig)
            .build();

        EventScheduler scheduler = new EventSchedulerBuilderInternal()
                .setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE))
                .setLogger(EventLoggerStdOut.INSTANCE_DEBUG)
                .build();

        assertNotNull(scheduler);
    }

    @Test
    public void stopAndAbort()  {

        EventSchedulerEngine eventSchedulerEngine = mock(EventSchedulerEngine.class);

        EventConfig eventConfig = EventConfig.builder()
            .name("myEvent")
            .enabled(true)
            .eventFactory("nl.stokpop.eventscheduler.event.EventFactoryDefault")
            .testConfig(TestConfig.builder().build())
            .build();

        EventSchedulerConfig config = EventSchedulerConfig.builder()
            .eventConfig(eventConfig)
            .build();

        EventScheduler scheduler = new EventSchedulerBuilderInternal()
            .setEventSchedulerContext(config.toContext(EventLoggerStdOut.INSTANCE))
            .setLogger(EventLoggerStdOut.INSTANCE_DEBUG)
            .setEventSchedulerEngine(eventSchedulerEngine)
            .build();

        scheduler.startSession();

        scheduler.startSession();

        scheduler.stopSession();

        scheduler.abortSession();

        // should be called only one time, also for multiple starts in a row
        Mockito.verify(eventSchedulerEngine, times(1)).startCustomEventScheduler(any(), any());

        // should be called once in stop, not also in abort
        Mockito.verify(eventSchedulerEngine, times(1)).shutdownThreadsNow();

    }
}
