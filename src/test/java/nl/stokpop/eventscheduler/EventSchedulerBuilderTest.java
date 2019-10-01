package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.PerfanaConnectionSettingsBuilder;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

public class EventSchedulerBuilderTest {

    @Test
    public void createWithAlternativeClass() {
         String alternativeClassCustomEvents = "  @generator-class=io.perfana.event.generator.EventScheduleGeneratorDefault \n" +
                 "  eventSchedule=PT1M|do-something \n";

         EventSchedulerBuilder eventSchedulerBuilder = new EventSchedulerBuilder()
                 .setCustomEvents(alternativeClassCustomEvents)
                 .setTestContext(new TestContextBuilder().build())
                 .setPerfanaConnectionSettings(new PerfanaConnectionSettingsBuilder().build());

        EventScheduler eventScheduler = eventSchedulerBuilder.build(new URLClassLoader(new URL[]{}, Thread.currentThread().getContextClassLoader()));

        // TODO what to assert?

    }

}