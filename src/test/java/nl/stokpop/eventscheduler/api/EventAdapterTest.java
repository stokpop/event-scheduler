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

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class EventAdapterTest {

    @Test
    public void testSetOf() {
        Set<String> items = EventAdapter.setOf("B", "B", "A");
        // expect unique
        assertEquals(2, items.size());
        // expect order
        String[] array = items.toArray(new String[0]);
        assertEquals("A", array[0]);
        assertEquals("B", array[1]);

    }
}