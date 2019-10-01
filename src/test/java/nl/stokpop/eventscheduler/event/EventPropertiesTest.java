package nl.stokpop.eventscheduler.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventPropertiesTest {

    @Test
    public void createEventProperties() {
        EventSchedulerProperties properties = new EventSchedulerProperties();

        String name = "my-name";
        String value = "my-value";
        properties.put("nl.stokpop.eventscheduler.event.EventPropertiesTest.MyEvent", name, value);

        assertEquals(value, properties.get(new MyEvent()).getProperty(name));
    }

    private static final class MyEvent extends EventAdapter {
        @Override
        public String getName() {
            return "MyCustomEvent";
        }
        // no further implementation needed for this test
    }

    @Test
    public void nonExistingProperties() {
        EventSchedulerProperties properties = new EventSchedulerProperties();
        assertTrue(properties.get(new MyEvent()).isEmpty());
    }

}