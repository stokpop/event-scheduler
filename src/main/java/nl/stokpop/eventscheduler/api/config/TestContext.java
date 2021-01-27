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
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.jcip.annotations.Immutable;

import java.time.Duration;
import java.util.List;

@Value
@Builder(access = AccessLevel.PROTECTED)
@Immutable
public class TestContext {
    String systemUnderTest;
    String workload;
    String testEnvironment;
    String productName;
    String dashboardName;
    String testRunId;
    String buildResultsUrl;
    String version;
    String annotations;
    @Singular
    List<String> tags;
    Duration rampupTime;
    Duration constantLoadTime;
}
