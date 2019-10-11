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