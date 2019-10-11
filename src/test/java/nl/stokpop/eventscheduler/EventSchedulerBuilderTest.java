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