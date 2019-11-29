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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adapter class with empty method implementations of the Event interface.
 * Extend this class so you only have to implement the methods that are used.
 *
 * Always provide a proper name for an Event for traceability.
 */
public abstract class EventAdapter implements Event {

    protected final String eventName;
    protected final TestContext testContext;
    protected final EventProperties eventProperties;
    protected final EventLogger logger;

    public EventAdapter(String eventName, TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        this.eventName = eventName;
        this.testContext = testContext;
        this.eventProperties = eventProperties;
        this.logger = logger;
    }

    @Override
    public void beforeTest() {
        logger.debug(String.format("[%s] [%s] beforeTest (not implemented)", eventName, this.getClass().getName()));
    }

    @Override
    public void afterTest() {
        logger.debug(String.format("[%s] [%s] afterTest (not implemented)", eventName, this.getClass().getName()));
    }

    @Override
    public void keepAlive() {
        logger.debug(String.format("[%s] [%s] keepAlive (not implemented)", eventName, this.getClass().getName()));
    }

    @Override
    public void abortTest() {
        logger.debug(String.format("[%s] [%s] abortTest (not implemented)", eventName, this.getClass().getName()));
    }

    @Override
    public EventCheck check() {
        return EventCheck.DEFAULT;
    }

    @Override
    public void customEvent(CustomEvent customEvent) {
        logger.debug(String.format("[%s] [%s] [%s] customEvent (not implemented)", eventName, this.getClass().getName(), customEvent.getName()));
    }

    @Override
    public final String getName() {
        return eventName;
    }

    /**
     * Convenience method for the allowed properties or events.
     * @param items the allowed props or events
     * @return unmodifiable and ordered set of items
     */
    public static Set<String> setOf(String... items) {
        // TreeSet is ordered
        return Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(items)));
    }

}
