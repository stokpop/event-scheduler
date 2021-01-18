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
package nl.stokpop.eventscheduler.api.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * The EventConfig is used is given to each event call.
 *
 * One required field is the 'eventFactory' that contains the fully qualified
 * class name of the factory class for the event.
 *
 * Another field is 'enabled', default is true.
 * If set to false, the event will not be active.
 */

//@Immutable
@Value
@AllArgsConstructor
@Builder(access = AccessLevel.PROTECTED)
@NonFinal
public class EventContext {
    String name;
    String eventFactory;
    boolean enabled;
    String scheduleScript;
    TestContext testContext;
    boolean isReadyForStartParticipant;

    protected EventContext(EventContext context, String eventFactory, boolean isReadyForStartParticipant) {
        this.name = context.name;
        this.eventFactory = eventFactory;
        this.enabled = context.enabled;
        this.scheduleScript = context.scheduleScript;
        this.testContext = context.testContext;
        this.isReadyForStartParticipant = isReadyForStartParticipant;
    }
}
