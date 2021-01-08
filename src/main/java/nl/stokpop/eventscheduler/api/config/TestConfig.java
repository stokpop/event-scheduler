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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestConfig {
    @Builder.Default
    private String systemUnderTest = "UNKNOWN_SYSTEM_UNDER_TEST";
    @Builder.Default
    private String workload = "UNKNOWN_WORKLOAD";
    @Builder.Default
    private String testEnvironment = "UNKNOWN_TEST_ENVIRONMENT";
    @Builder.Default
    private String productName = "ANONYMOUS_PRODUCT";
    @Builder.Default
    private String dashboardName = "ANONYMOUS_DASHBOARD";
    @Builder.Default
    private String testRunId = "ANONYMOUS_TEST_ID";
    private String buildResultsUrl;
    @Builder.Default
    private String version = "1.0.0-SNAPSHOT";
    private String annotations;
    private Map<String,String> variables;
    private List<String> tags;
    @Builder.Default
    private Integer rampupTimeInSeconds = 30;
    @Builder.Default
    private Integer constantLoadTimeInSeconds = 570;
}
