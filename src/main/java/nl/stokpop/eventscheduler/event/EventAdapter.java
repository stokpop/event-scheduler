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

import nl.stokpop.eventscheduler.api.TestContext;

/**
 * Adapter class with empty method implementations of the Event interface.
 * Extend this class so you only have to implement the methods that are used.
 *
 * Always provide a proper name for an Event for traceability.
 */
public abstract class EventAdapter implements Event {

    @Override
    public void beforeTest(TestContext context, EventProperties properties) {

    }

    @Override
    public void afterTest(TestContext context, EventProperties properties) {

    }

    @Override
    public void keepAlive(TestContext context, EventProperties properties) {

    }

    @Override
    public void abortTest(TestContext context, EventProperties properties) {

    }

    @Override
    public void checkTest(TestContext context, EventProperties properties) {

    }

    @Override
    public void customEvent(TestContext context, EventProperties properties, ScheduleEvent scheduleEvent) {

    }
}
