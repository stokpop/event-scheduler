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

import nl.stokpop.eventscheduler.api.EventSchedulerLoggerStdOut;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import nl.stokpop.eventscheduler.event.EventBroadcaster;
import nl.stokpop.eventscheduler.event.EventSchedulerProperties;
import nl.stokpop.eventscheduler.event.ScheduleEvent;
import nl.stokpop.eventscheduler.generator.EventGeneratorDefault;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class EventSchedulerEngineTest {

    @Test
    public void createEventScheduleMessage() {

        String eventsAsText =
                "PT1M|event(my description: phase 1(foo))|settings=true\n" +
                "PT2M|event(my description: phase 2(bar))|settings=true\n" +
                "PT3M|event(my description: phase 3(very long event description test))|settings=true";

        List<ScheduleEvent> events = new EventGeneratorDefault().createTestEvents(eventsAsText);

        String eventScheduleMessage = EventSchedulerEngine.createEventScheduleMessage(events);

        System.out.println(eventScheduleMessage);
        String search = "phase";
        assertEquals(3, EventSchedulerUtils.countOccurrences(search, eventScheduleMessage));
    }

    @Test
    public void runMultipleEventsWithExceptions() throws InterruptedException {

        List<ScheduleEvent> events = new ArrayList<>();
        events.add(ScheduleEvent.createFromLine("PT0.1S|my-event(phase 1)"));
        events.add(ScheduleEvent.createFromLine("PT0.2S|my-event(phase 2)"));
        events.add(ScheduleEvent.createFromLine("PT0.3S|my-event(phase 3)"));
        events.add(ScheduleEvent.createFromLine("PT0.4S|my-event(phase 4)"));
        events.add(ScheduleEvent.createFromLine("PT0.5S|my-event(phase 5)"));

        EventSchedulerEngine engine = new EventSchedulerEngine(new EventSchedulerLoggerStdOut());

        TestContext context = new TestContextBuilder().build();

        EventBroadcaster eventBroadcaster = mock(EventBroadcaster.class);
        // expect 5 calls, two will throw an Exception, see if flow continues
        doThrow(new EventSchedulerRuntimeException("help! broadcastCustomEvent error!"))
                .doNothing()
                .doNothing()
                .doThrow(new EventSchedulerRuntimeException("help! broadcastCustomEvent error!"))
                .doNothing()
                .when(eventBroadcaster).broadcastCustomEvent(any(), any(), any());

        EventSchedulerProperties eventProperties = new EventSchedulerProperties();
        engine.startCustomEventScheduler(context, events, eventBroadcaster, eventProperties);

        // check if all events are called at 100, 200, 300, 400 and 500 ms
        Thread.sleep(600);

        engine.shutdownThreadsNow();

        verify(eventBroadcaster, times(5))
                .broadcastCustomEvent(eq(context), eq(eventProperties), any(ScheduleEvent.class));
    }

}