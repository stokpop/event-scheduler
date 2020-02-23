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

import jdk.nashorn.internal.ir.annotations.Immutable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Information for a (performance) test run.
 */
@Immutable
public class TestContext {

    private final String application;
    private final String testType;
    private final String testEnvironment;
    private final String testRunId;
    private final String applicationRelease;
    private final String CIBuildResultsUrl;
    private final Duration rampupTime;
    private final Duration plannedDuration;
    private final String annotations;
    private final Map<String, String> variables;
    private final List<String> tags;

    TestContext(String application, String testType, String testEnvironment, String testRunId, String CIBuildResultsUrl, String applicationRelease, Duration rampupTime, Duration plannedDuration, String annotations, Map<String, String> variables, List<String> tags) {
        this.application = application;
        this.testType = testType;
        this.testEnvironment = testEnvironment;
        this.testRunId = testRunId;
        this.CIBuildResultsUrl = CIBuildResultsUrl;
        this.applicationRelease = applicationRelease;
        this.rampupTime = rampupTime;
        this.plannedDuration = plannedDuration;
        this.annotations = annotations;
        this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public String getApplication() {
        return application;
    }

    public String getTestType() {
        return testType;
    }

    public String getTestEnvironment() {
        return testEnvironment;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public String getCIBuildResultsUrl() {
        return CIBuildResultsUrl;
    }

    public String getApplicationRelease() {
        return applicationRelease;
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
