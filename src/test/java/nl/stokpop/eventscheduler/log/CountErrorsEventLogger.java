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
package nl.stokpop.eventscheduler.log;

import nl.stokpop.eventscheduler.api.EventLogger;

import java.util.concurrent.atomic.AtomicInteger;

public class CountErrorsEventLogger implements EventLogger {

    private EventLogger wrappedEventLogger;

    private AtomicInteger errorCounter = new AtomicInteger(0);

    private CountErrorsEventLogger(EventLogger wrappedEventLogger) {
        this.wrappedEventLogger = wrappedEventLogger;
    }

    public static CountErrorsEventLogger of(EventLogger eventLogger) {
        return new CountErrorsEventLogger(eventLogger);
    }

    @Override
    public void info(String message) {
        wrappedEventLogger.info(message);
    }

    @Override
    public void warn(String message) {
        wrappedEventLogger.warn(message);
    }

    @Override
    public void error(String message) {
        errorCounter.incrementAndGet();
        wrappedEventLogger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        errorCounter.incrementAndGet();
        wrappedEventLogger.error(message, throwable);
    }

    @Override
    public void debug(String message) {
        wrappedEventLogger.debug(message);
    }

    @Override
    public boolean isDebugEnabled() {
        return wrappedEventLogger.isDebugEnabled();
    }

    public int errorCount() {
        return errorCounter.get();
    }
}
