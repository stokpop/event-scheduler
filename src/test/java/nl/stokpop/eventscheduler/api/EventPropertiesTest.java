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
package nl.stokpop.eventscheduler.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;


public class EventPropertiesTest {


    @Test
    public void checkUnknownProperties() {
        EventProperties eventProperties = createEventProperties();

        Collection<String> allowedProps = new ArrayList<>();
        allowedProps.add("allowedKey");

        Map<String, String> test = new HashMap<>();
        eventProperties.checkUnknownProperties(allowedProps, test::put);
        assertTrue(test.containsKey("unknownKey"));
    }

    @Test
    public void containsUnknownProperties() {
        EventProperties eventProperties = createEventProperties();

        Collection<String> allowedProps = new ArrayList<>();
        allowedProps.add("allowedKey");

        assertTrue(eventProperties.containsUnknownProperties(allowedProps));
    }

    private EventProperties createEventProperties() {
        Map<String, String> props = new HashMap<>();
        props.put(EventProperties.PROP_FACTORY_CLASSNAME, "classname");
        props.put("allowedKey", "oneValue");
        props.put("unknownKey", "twoValue");
        return new EventProperties(props);
    }
}
