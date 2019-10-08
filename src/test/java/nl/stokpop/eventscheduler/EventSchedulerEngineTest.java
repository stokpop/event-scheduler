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

        final AtomicInteger broadcastCount = new AtomicInteger(0);

        EventBroadcaster broadcaster = new EventBroadcaster() {
            @Override
            public void broadcastBeforeTest(TestContext context, EventSchedulerProperties eventProperties) {
                System.out.println("broadcast: before test");
            }

            @Override
            public void broadcastAfterTest(TestContext context, EventSchedulerProperties eventProperties) {
                System.out.println("broadcast: after test");
            }

            @Override
            public void broadCastKeepAlive(TestContext context, EventSchedulerProperties eventProperties) {
                System.out.println("broadcast: keep alive");
            }

            @Override
            public void broadcastAbortTest(TestContext context, EventSchedulerProperties eventProperties) {
                System.out.println("broadcast: abort test");
            }

            @Override
            public void broadcastCustomEvent(TestContext context, EventSchedulerProperties eventProperties, ScheduleEvent event) {
                System.out.println("broadcast: custom event: " + event);
                broadcastCount.incrementAndGet();
                if (broadcastCount.intValue() < 3) {
                    throw new EventSchedulerRuntimeException("help! broadcastCustomEvent: " + event) ;
                }
            }

            @Override
            public void broadcastCheckResults(TestContext context, EventSchedulerProperties eventProperties) {
                System.out.println("broadcast: check results");
            }
        };

        engine.startCustomEventScheduler(context, events, broadcaster, new EventSchedulerProperties());

        // check if all events are called
        Thread.sleep(600);

        engine.shutdownThreadsNow();

        assertEquals("expected 5 broadcast calls", 5, broadcastCount.intValue());
    }

}