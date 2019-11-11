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

import nl.stokpop.eventscheduler.api.Event;
import nl.stokpop.eventscheduler.api.EventFactory;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.EventProperties;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;

public class EventFactoryDefault implements EventFactory {

    private String name;

    public EventFactoryDefault() {
        this("EventFactoryDefault default constructor instance");
    }

    EventFactoryDefault(String name) {
        this.name = name;
    }

    @Override
    public Event create(String eventName, TestContext context, EventProperties properties, EventLogger logger) {
        return new EventDefault(eventName, context, properties, EventLoggerStdOut.INSTANCE);
    }

    @Override
    public String toString() {
        return "EventFactoryDefault (" + name + ")";
    }
}
