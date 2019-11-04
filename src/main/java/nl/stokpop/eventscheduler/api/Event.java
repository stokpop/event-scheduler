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

/**
 * This interface can be implemented in other jars and be put on the classpath.
 *
 * Provide a file in META-INF/services/nl.stokpop.eventscheduler.api.Event that contains the
 * fully qualified name of the implementing class.
 *
 * This class will be used when these events are called. Possibly you can even provide multiple implementations
 * on the classpath that will all be called.
 *
 * For more information on how this technically works, check out javadoc of java.util.ServiceLoader.
 */
public interface Event {

    /**
     * @return name of the test event.
     */
    String getName();

    /**
     * Called before the test run starts. You can for instance cleanup the test environment and/or
     * restart the server under test.
     */
    void beforeTest();

    /**
     * Called after the test run is done. Use for instance to start creating a report of some sort or
     * remove the test environment.
     */
    void afterTest();
    
    /**
     * Called for each keep alive event for this test run.
     */
    void keepAlive();

    /**
     * Called for test abort.
     */
    void abortTest();

    /**
     * Called to check test results (for example the requirements are checked).
     * Can be used to have a test run fail or succeed in continuous integration setups.
     */
    EventCheck check();

    /**
     * Called for each custom event, according to the custom even schedule.
     * @param scheduleEvent the custom event, use to execute specific behaviour in the event handler
     */
    void customEvent(CustomEvent scheduleEvent);


}
