package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.EventSchedulerSettingsBuilder;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

public class EventSchedulerBuilderTest {

    @Test
    public void createWithAlternativeClass() {
         String alternativeClassCustomEvents = "  @generator-class=nl.stokpop.eventscheduler.generator.EventGeneratorDefault \n" +
                 "  eventSchedule=PT1M|do-something \n";

         EventSchedulerBuilder eventSchedulerBuilder = new EventSchedulerBuilder()
                 .setCustomEvents(alternativeClassCustomEvents)
                 .setTestContext(new TestContextBuilder().build())
                 .setEventSchedulerSettings(new EventSchedulerSettingsBuilder().build());

        EventScheduler eventScheduler = eventSchedulerBuilder.build(new URLClassLoader(new URL[]{}, Thread.currentThread().getContextClassLoader()));

        // TODO what to assert?

    }

}