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
package nl.stokpop.eventscheduler.log;

import nl.stokpop.eventscheduler.api.EventLogger;

public class EventLoggerWithName implements EventLogger {

    private final String name;
    private final String classname;
    private final EventLogger logger;

    public EventLoggerWithName(String name, String classname, EventLogger logger) {
        this.name = name;
        this.classname = removePackages(classname);
        this.logger = logger;
    }

    private String removePackages(String classname) {
        if (classname.contains(".")) {
            return classname.substring(classname.lastIndexOf('.') + 1);
        }
        else {
            return classname;
        }
    }


    @Override
    public void info(String message) {
        logger.info(String.format("[%s] [%s] %s", name, classname, message));
    }

    @Override
    public void warn(String message) {
        logger.warn(String.format("[%s] [%s] %s", name, classname, message));
    }

    @Override
    public void error(String message) {
        logger.error(String.format("[%s] [%s] %s", name, classname, message));
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(String.format("[%s] [%s] %s", name, classname, message), throwable);
    }

    @Override
    public void debug(String message) {
        logger.debug(String.format("[%s] [%s] %s", name, classname, message));
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }
}
