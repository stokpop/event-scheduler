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
package nl.stokpop.eventscheduler.api;

import net.jcip.annotations.NotThreadSafe;
import nl.stokpop.eventscheduler.EventSchedulerUtils;

import java.time.Duration;

@NotThreadSafe
public class EventSchedulerSettingsBuilder {

    private static final int DEFAULT_KEEP_ALIVE_TIME_SECONDS = 30;
    private Duration keepAliveInterval = Duration.ofSeconds(DEFAULT_KEEP_ALIVE_TIME_SECONDS);

    public EventSchedulerSettingsBuilder setKeepAliveTimeInSeconds(String keepAliveTimeInSeconds) {
        this.keepAliveInterval = Duration.ofSeconds(EventSchedulerUtils.parseInt("keepAliveTimeInSeconds", keepAliveTimeInSeconds, DEFAULT_KEEP_ALIVE_TIME_SECONDS));
        return this;
    }

    public EventSchedulerSettingsBuilder setKeepAliveInterval(Duration keepAliveInterval) {
        if (keepAliveInterval != null) {
            this.keepAliveInterval = keepAliveInterval;
        }
        return this;
    }
    
    public EventSchedulerSettings build() {
        return new EventSchedulerSettings(keepAliveInterval);
    }

}