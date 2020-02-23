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
package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.CustomEvent;
import nl.stokpop.eventscheduler.api.EventAdapter;
import nl.stokpop.eventscheduler.api.EventCheck;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.EventProperties;
import nl.stokpop.eventscheduler.api.TestContext;

public class EventDefault extends EventAdapter {

    EventDefault(String eventName, TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        super(eventName, testContext, eventProperties, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("Before test: " + testContext.getTestRunId());
    }

    @Override
    public EventCheck check() {
        return EventCheck.DEFAULT;
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {

    }
}
