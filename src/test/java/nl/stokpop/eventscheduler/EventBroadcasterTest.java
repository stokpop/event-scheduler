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

import nl.stokpop.eventscheduler.api.*;
import nl.stokpop.eventscheduler.exception.KillSwitchException;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class EventBroadcasterTest {

    @Test
    public void broadcastAbort() {
        Event myEvent = mock(Event.class);
        CustomEvent scheduleEvent = mock(CustomEvent.class);

        List<Event> events = new ArrayList<>();
        events.add(myEvent);

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);

        broadcaster.broadcastBeforeTest();
        broadcaster.broadcastKeepAlive();
        broadcaster.broadcastCustomEvent(scheduleEvent);
        broadcaster.broadcastCheck();
        broadcaster.broadcastAbortTest();

        broadcaster.shutdownAndWaitAllTasksDone(2);
        
        verify(myEvent, times(1)).beforeTest();
        verify(myEvent, times(1)).keepAlive();
        verify(myEvent, times(1)).customEvent(scheduleEvent);
        verify(myEvent, times(1)).check();
        verify(myEvent, times(1)).abortTest();
    }

    @Test
    public void broadcastCustomEventWithFailureShouldProceed() {

        // not multi-threading code, but used as a convenience to change an object in the inner classes below
        // beware: expects a certain order for the events to be called, which can be different depending on implementation
        final AtomicInteger counter = new AtomicInteger(0);

        List<Event> events = new ArrayList<>();
        // this should succeed
        events.add(new MyTestEventThatCanFail(counter, 0, 1));
        // this will fail: counter is 0
        events.add(new MyTestEventThatCanFail(counter, 10, 11));
        // this should succeed
        events.add(new MyTestEventThatCanFail(counter, 1, 2));

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);

        broadcaster.broadcastCustomEvent(CustomEvent.createFromLine("PT1M|test-event"));

        broadcaster.shutdownAndWaitAllTasksDone(2);

        assertEquals("counter should be set to 2 even though the middle event failed", 2, counter.intValue());
    }

    private static class MyTestEventThatCanFail extends EventAdapter {
        // just because it is needed...
        private final static TestContext testContext = new TestContextBuilder().build();
        private final static EventProperties eventProperties = new EventProperties();

        private AtomicInteger counter;
        private int expectValue;
        private int newValue;


        MyTestEventThatCanFail(AtomicInteger counter, int expectValue, int newValue) {
            super("MyTestEventThatCanFail", testContext, eventProperties, EventLoggerStdOut.INSTANCE);
            this.counter = counter;
            this.expectValue= expectValue;
            this.newValue = newValue;
        }

        @Override
        public void customEvent(CustomEvent customEvent) {
            if (!counter.compareAndSet(expectValue, newValue)) throw new RuntimeException("counter was not " + expectValue);
        }
    }

    @Test
    public void broadcastTakesTooLongBehaviourBeforeTest() {
        // what happens when events "hijack" the event thread?
        List<Event> events = createTestEvents();

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);
        long startTime = System.currentTimeMillis();
        // blocks to wait for results, but this should not take longer
        // than the longest wait time of a task
        broadcaster.broadcastBeforeTest();
        long durationMillis = System.currentTimeMillis() - startTime;

        sleep(1000);
        
        assertTrue("should not take more than a 300 millis! actual: " + durationMillis, durationMillis < 300);

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test
    public void broadcastTakesTooLongBehaviourCheck() {
        // what happens when events "hijack" the event thread?
        List<Event> events = createTestEvents();

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);

        long startTime = System.currentTimeMillis();
        List<EventCheck> eventChecks = broadcaster.broadcastCheck();
        long durationMillis = System.currentTimeMillis() - startTime;

        assertEquals(4, eventChecks.size());
        assertEquals(1, eventChecks.stream().filter(e -> e.getEventStatus() == EventStatus.FAILURE).count());
        assertEquals(3, eventChecks.stream().filter(e -> e.getEventStatus() == EventStatus.SUCCESS).count());

        assertTrue("should not take more than 600 millis: " + durationMillis, durationMillis < 600);

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    @Test(expected = KillSwitchException.class)
    public void broadcastKeepAliveWithKillSwitchException() {
        // what happens when an event throws a KillSwitchException?
        List<Event> events = createKillSwitchTestEvents();

        EventBroadcaster broadcaster = new EventBroadcasterAsync(events, EventLoggerStdOut.INSTANCE);

        broadcaster.broadcastKeepAlive();

        broadcaster.shutdownAndWaitAllTasksDone(2);
    }

    private List<Event> createTestEvents() {
        MySleepyEvent sleepyEvent1 = new MySleepyEvent("sleepy1");
        MySleepyEvent sleepyEvent2 = new MySleepyEvent("sleepy2");
        MySleepyEvent sleepyEvent3 = new MySleepyEvent("sleepy3");
        MyErrorEvent errorEvent = new MyErrorEvent("error1");

        List<Event> events = new ArrayList<>();
        events.add(sleepyEvent1);
        events.add(sleepyEvent2);
        events.add(sleepyEvent3);
        events.add(errorEvent);
        return events;
    }

    private List<Event> createKillSwitchTestEvents() {
        MyKillSwitchEvent killSwitchEvent1 = new MyKillSwitchEvent("no-killer");
        MyKillSwitchEvent killSwitchEvent2 = new MyKillSwitchEvent("killer-one");

        List<Event> events = new ArrayList<>();
        events.add(killSwitchEvent1);
        events.add(killSwitchEvent2);
        return events;
    }

    private static class MySleepyEvent extends EventAdapter {
        // just because it is needed...
        private final static TestContext testContext = new TestContextBuilder().build();
        private final static EventProperties eventProperties = new EventProperties();

        public MySleepyEvent(String eventName) {
            super(eventName, testContext, eventProperties, EventLoggerStdOut.INSTANCE_DEBUG);
        }

        @Override
        public void beforeTest() {
            logger.info(System.currentTimeMillis() + " Sleep in before test in thread: " + Thread.currentThread().getName());
            sleep(200);
            logger.info(System.currentTimeMillis() + " After sleep in before test in thread: " + Thread.currentThread().getName());
        }

        @Override
        public EventCheck check() {
            logger.info(System.currentTimeMillis() + " Sleep in check in thread: " + Thread.currentThread().getName());
            sleep(500);
            logger.error(System.currentTimeMillis() + " After sleep in check in thread: " + Thread.currentThread().getName());
             return new EventCheck(eventName, getClass().getSimpleName(), EventStatus.SUCCESS, "All ok");
        }
    }

    private static class MyKillSwitchEvent extends EventAdapter {
        // just because it is needed...
        private final static TestContext testContext = new TestContextBuilder().build();
        private final static EventProperties eventProperties = new EventProperties();

        public MyKillSwitchEvent(String eventName) {
            super(eventName, testContext, eventProperties, EventLoggerStdOut.INSTANCE_DEBUG);
        }

        @Override
        public void keepAlive() {
            logger.info("keep alive called for " + eventName);
            if (eventName.startsWith("killer")) {
                throw new KillSwitchException("kill switch in action from " + eventName);
            }
        }
    }

    private static class MyErrorEvent extends EventAdapter {
        // just because it is needed...
        private final static TestContext testContext = new TestContextBuilder().build();
        private final static EventProperties eventProperties = new EventProperties();

        public MyErrorEvent(String eventName) {
            super(eventName, testContext, eventProperties, EventLoggerStdOut.INSTANCE_DEBUG);
        }

        @Override
        public void beforeTest() {
            logger.info(System.currentTimeMillis() + " Sleep in before test error in thread: " + Thread.currentThread().getName());
            sleep(200);
            logger.info(System.currentTimeMillis() + " After sleep in test error in thread: " + Thread.currentThread().getName());
            throw new RuntimeException("oops, something went wrong in before test!");
        }

        @Override
        public EventCheck check() {
            logger.info(System.currentTimeMillis() + " Sleep in error check in thread: " + Thread.currentThread().getName());
            sleep(500);
            logger.error(System.currentTimeMillis() + " After sleep in error check in thread: " + Thread.currentThread().getName());
            throw new RuntimeException("oops, something went wrong in check!");
        }
    }

    private static void sleep(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            System.out.println("interrupt received: " + Thread.currentThread().getName());
            Thread.currentThread().interrupt();        }
    }

}