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

import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.EventSchedulerSettingsBuilder;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.api.TestContextBuilder;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventSchedulerBuilderTest {

    @Test
    public void createWithAlternativeClass() {
         String alternativeClassCustomEvents = "  @generatorFactoryClass=nl.stokpop.eventscheduler.generator.EventGeneratorFactoryDefault\n" +
                 "  my-setting=my-value \n";

         EventSchedulerBuilder eventSchedulerBuilder = new EventSchedulerBuilder()
                 .setCustomEvents(alternativeClassCustomEvents)
                 .setTestContext(new TestContextBuilder().build())
                 .setEventSchedulerSettings(new EventSchedulerSettingsBuilder().build());

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        EventScheduler eventScheduler =
                eventSchedulerBuilder.build(new URLClassLoader(new URL[]{}, classLoader));

        eventScheduler.checkResults();
        eventScheduler.abortSession();
    }

    /**
     * Example pom input:
     * <pre>
     * {@code
     * <nl.stokpop.event.wiremock.WiremockEventFactory>
     * 	<wiremockEnabled>true</wiremockEnabled>
     * 	<wiremockFilesDir>src/test/resources/wiremock</wiremockFilesDir>
     * 	<wiremockUrl>https://site.a/</wiremockUrl>
     * </nl.stokpop.event.wiremock.WiremockEventFactory>
     *
     * <nl.stokpop.event.wiremock.WiremockEventFactory>
     * 	<wiremockEnabled>true</wiremockEnabled>
     * 	<wiremockFilesDir>src/test/resources/wiremock</wiremockFilesDir>
     * 	<wiremockUrl>https://site.b/</wiremockUrl>
     * </nl.stokpop.event.wiremock.WiremockEventFactory>
     * }
     * </pre>
     *
     */
    @Test
    public void testFactoriesAndBroadcaster() {
        EventSchedulerBuilder builder = new EventSchedulerBuilder();

        TestContext testContext = new TestContextBuilder().build();

        EventSchedulerSettingsBuilder eventSchedulerSettingsBuilder = new EventSchedulerSettingsBuilder();
        EventSchedulerSettings eventSchedulerSettings = eventSchedulerSettingsBuilder.build();

        builder.setTestContext(testContext);
        builder.setEventSchedulerSettings(eventSchedulerSettings);

        String factoryClassName = "nl.stokpop.eventscheduler.event.EventFactoryDefault";

        Map<String, String> properties = new HashMap<>();
        properties.put("eventFactory", factoryClassName);
        properties.put("wiremockEnabled", "true");
        properties.put("wiremockFilesDir", "src/test/resources/wiremock");
        properties.put("wiremockUrl", "https://site.a/");

        builder.addEvent("Event1", properties);

        Map<String, String> properties2 = new HashMap<>();
        properties2.put("eventFactory", factoryClassName);
        properties2.put("wiremockEnabled", "true");
        properties2.put("wiremockFilesDir", "src/test/resources/wiremock");
        properties2.put("wiremockUrl", "https://site.b/");

        builder.addEvent("Event2", properties);

        EventScheduler eventScheduler = builder.build();

        eventScheduler.startSession();
    }

    @Test(expected = EventSchedulerRuntimeException.class)
    public void testUniqueEventNameCheck() {
        EventSchedulerBuilder builder = new EventSchedulerBuilder();
        builder.addEvent("one", Collections.emptyMap());
        builder.addEvent("one", Collections.emptyMap());
    }

}