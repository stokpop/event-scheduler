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

import nl.stokpop.eventscheduler.api.CustomEvent;
import nl.stokpop.eventscheduler.api.EventGenerator;
import nl.stokpop.eventscheduler.api.EventGeneratorProperties;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import nl.stokpop.eventscheduler.generator.EventGeneratorDefault;
import nl.stokpop.eventscheduler.generator.EventGeneratorFactoryDefault;
import nl.stokpop.eventscheduler.log.CountErrorsEventLogger;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class EventSchedulerEngineTest {

    @Test
    public void createEventScheduleMessage() {

        String eventsAsText =
                "PT1M|event(my description: phase 1(foo))|settings=true\n" +
                "PT2M|event(my description: phase 2(bar))|settings=true\n" +
                "PT3M|event(my description: phase 3(very long event description test))|settings=true";
        Map<String, String> map = new HashMap<>();
        map.put(EventGeneratorDefault.EVENT_SCHEDULE_TAG, eventsAsText);
        EventGeneratorProperties generatorProperties = new EventGeneratorProperties(map);

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);

        EventGenerator eventGenerator = new EventGeneratorFactoryDefault()
                .create(generatorProperties, countErrorsEventLogger);

        String eventScheduleMessage = EventSchedulerEngine.createEventScheduleMessage(eventGenerator.generate());

        System.out.println(eventScheduleMessage);
        String search = "phase";
        assertEquals(3, EventSchedulerUtils.countOccurrences(search, eventScheduleMessage));
        assertEquals("zero errors expected in logger", 0, countErrorsEventLogger.errorCount());
    }

    @Test
    public void runMultipleEventsWithExceptions() throws InterruptedException {

        List<CustomEvent> events = new ArrayList<>();
        events.add(CustomEvent.createFromLine("PT0.1S|my-event(phase 1)"));
        events.add(CustomEvent.createFromLine("PT0.2S|my-event(phase 2)"));
        events.add(CustomEvent.createFromLine("PT0.3S|my-event(phase 3)"));
        events.add(CustomEvent.createFromLine("PT0.4S|my-event(phase 4)"));
        events.add(CustomEvent.createFromLine("PT0.5S|my-event(phase 5)"));

        CountErrorsEventLogger countErrorsEventLogger = CountErrorsEventLogger.of(EventLoggerStdOut.INSTANCE);
        EventSchedulerEngine engine = new EventSchedulerEngine(countErrorsEventLogger);

        EventBroadcaster eventBroadcaster = mock(EventBroadcaster.class);
        // expect 5 calls, two will throw an Exception, see if flow continues
        doThrow(new EventSchedulerRuntimeException("help! broadcastCustomEvent error!"))
                .doNothing()
                .doNothing()
                .doThrow(new EventSchedulerRuntimeException("help! broadcastCustomEvent error!"))
                .doNothing()
                .when(eventBroadcaster).broadcastCustomEvent(any());

        engine.startCustomEventScheduler(events, eventBroadcaster);

        // check if all events are called at 100, 200, 300, 400 and 500 ms
        Thread.sleep(600);

        engine.shutdownThreadsNow();

        verify(eventBroadcaster, times(5))
                .broadcastCustomEvent(any(CustomEvent.class));
        assertEquals("two errors expected in logger", 2, countErrorsEventLogger.errorCount());
    }

}