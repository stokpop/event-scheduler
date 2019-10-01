package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.EventScheduler;
import nl.stokpop.eventscheduler.EventSchedulerBuilder;
import nl.stokpop.eventscheduler.api.EventSchedulerLogger;
import nl.stokpop.eventscheduler.api.EventSchedulerLoggerStdOut;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.EventSchedulerSettingsBuilder;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Collections;
import java.util.Arrays;


import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This test class is in another package to check access package private fields.
 */
public class EventSchedulerTest
{
    @Test
    public void create() {

        EventSchedulerLogger testLogger = new EventSchedulerLoggerStdOut();

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
                .setTags("")
                .build();

        EventScheduler client = new EventSchedulerBuilder()
                .setEventSchedulerSettings(settings)
                .setTestContext(context)
                .setAssertResultsEnabled(true)
                .addEventProperty("myClass", "name", "value")
                .setCustomEvents(eventSchedule)
                .setLogger(testLogger)
                .build();

        assertNotNull(client);
        assertEquals(120, settings.getKeepAliveDuration().getSeconds());

//        client.startSession();
//        client.stopSession();
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
                .setBroadcaster(null)
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
