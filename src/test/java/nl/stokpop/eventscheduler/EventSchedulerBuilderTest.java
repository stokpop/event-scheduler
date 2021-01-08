/*
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
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
import nl.stokpop.eventscheduler.api.config.EventConfig;
import nl.stokpop.eventscheduler.api.config.TestConfig;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

public class EventSchedulerBuilderTest {

    @Test
    public void createWithAlternativeClass() {
         String alternativeClassCustomEvents = "  @generatorFactoryClass=nl.stokpop.eventscheduler.generator.EventGeneratorFactoryDefault\n" +
                 "  my-setting=my-value \n";

         EventSchedulerBuilderInternal eventSchedulerBuilder = new EventSchedulerBuilderInternal()
             .setName("test-run-1")
             .setCustomEvents(alternativeClassCustomEvents)
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
        EventSchedulerBuilderInternal builder = new EventSchedulerBuilderInternal();

        EventSchedulerSettingsBuilder eventSchedulerSettingsBuilder = new EventSchedulerSettingsBuilder();
        EventSchedulerSettings eventSchedulerSettings = eventSchedulerSettingsBuilder.build();

        builder.setName("test-run-1");
        builder.setEventSchedulerSettings(eventSchedulerSettings);

        String factoryClassName = "nl.stokpop.eventscheduler.event.EventFactoryDefault";

        TestConfig testConfig = TestConfig.builder().build();

        EventConfig eventConfig = new EventConfig();
        eventConfig.setName("Event1");
        eventConfig.setEventFactory(factoryClassName);
        eventConfig.setEnabled(true);
        eventConfig.setTestConfig(testConfig);

        builder.addEvent(eventConfig);

        EventConfig eventConfig2 = new EventConfig();
        eventConfig2.setName("Event2");
        eventConfig2.setEventFactory(factoryClassName);
        eventConfig2.setEnabled(true);
        eventConfig2.setTestConfig(testConfig);

        builder.addEvent(eventConfig2);

        EventScheduler eventScheduler = builder.build();

        eventScheduler.startSession();
    }

    @Test(expected = EventSchedulerRuntimeException.class)
    public void testUniqueEventNameCheck() {
        EventSchedulerBuilderInternal builder = new EventSchedulerBuilderInternal();
        builder.addEvent(EventConfig.builder().name("one").build());
        builder.addEvent(EventConfig.builder().name("one").build());
    }

}