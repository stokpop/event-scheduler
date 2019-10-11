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
package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.EventSchedulerLoggerStdOut;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventProviderTest {

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

        EventProvider provider = new EventProvider(events, new EventSchedulerLoggerStdOut());

        provider.broadcastCustomEvent(new TestContextBuilder().build(), new EventSchedulerProperties(), ScheduleEvent.createFromLine("PT1M|test-event"));

        assertEquals("counter should be set to 2 even though the middle event failed", 2, counter.intValue());
    }

    private static class MyTestEventThatCanFail extends EventAdapter {
        private AtomicInteger counter;
        private int expectValue;
        private int newValue;
        MyTestEventThatCanFail(AtomicInteger counter, int expectValue, int newValue) {
            this.counter = counter;
            this.expectValue= expectValue;
            this.newValue = newValue;
        }
        @Override
        public String getName() {
            return "MyTestEventThatCanFail";
        }
        @Override
        public void customEvent(TestContext context, EventProperties properties, ScheduleEvent scheduleEvent) {
            if (!counter.compareAndSet(expectValue, newValue)) throw new RuntimeException("counter was not " + expectValue);
        }
    }

}