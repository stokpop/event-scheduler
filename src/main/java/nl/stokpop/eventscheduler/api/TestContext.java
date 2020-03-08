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
package nl.stokpop.eventscheduler.api;

import net.jcip.annotations.Immutable;

import java.time.Duration;
import java.util.*;

/**
 * Information for a (performance) test run.
 */
@Immutable
public class TestContext {

    private final String systemUnderTest;
    private final String workload;
    private final String environment;
    private final String testRunId;
    private final String version;
    private final String CIBuildResultsUrl;
    private final Duration rampupTime;
    private final Duration plannedDuration;
    private final String annotations;
    private final Map<String, String> variables;
    private final List<String> tags;

    TestContext(String systemUnderTest, String workload, String environment, String testRunId, String CIBuildResultsUrl, String version, Duration rampupTime, Duration plannedDuration, String annotations, Map<String, String> variables, List<String> tags) {
        this.systemUnderTest = systemUnderTest;
        this.workload = workload;
        this.environment = environment;
        this.testRunId = testRunId;
        this.CIBuildResultsUrl = CIBuildResultsUrl;
        this.version = version;
        this.rampupTime = rampupTime;
        this.plannedDuration = plannedDuration;
        this.annotations = annotations;
        this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public String getSystemUnderTest() {
        return systemUnderTest;
    }

    public String getWorkload() {
        return workload;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public String getCIBuildResultsUrl() {
        return CIBuildResultsUrl;
    }

    public String getVersion() {
        return version;
    }
    
    public Duration getRampupTime() {
        return rampupTime;
    }

    public Duration getPlannedDuration() {
        return plannedDuration;
    }

    public String getAnnotations() {
        return annotations;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public List<String> getTags() { return tags; }

}
