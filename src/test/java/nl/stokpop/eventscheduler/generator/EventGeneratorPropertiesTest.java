/*
 * Copyright (C) 2020 Peter Paul Bakker, Stokpop Software Solutions
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
package nl.stokpop.eventscheduler.generator;

import nl.stokpop.eventscheduler.api.EventGeneratorProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventGeneratorPropertiesTest {

    @Test
    public void retrieveProperties() {
        String text = "    @generatorFactoryClass    =     bar  \n" +
                "   my-prop    = foo   \n";

        EventGeneratorProperties props = new EventGeneratorProperties(text);

        // expect the properties to be visible in toString()
        assertTrue(props.toString().contains("bar"));
        assertTrue(props.toString().contains("foo"));
        
        assertEquals("bar", props.getMetaProperty("generatorFactoryClass"));
        assertEquals("foo", props.getProperty("my-prop"));
        

    }

}